package org.example.project

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.russhwolf.settings.Settings
import org.example.project.data.local.LocalSettingsRepository
import org.example.project.data.model.User
import org.example.project.presentation.*
import org.example.project.ui.navigation.NavRoutes
import org.example.project.ui.screens.auth.*
import org.example.project.ui.screens.landlord.*
import org.example.project.ui.screens.tenant.*
import org.example.project.ui.theme.RentOutTheme

@Composable
fun App() {
    RentOutTheme {
        val navController = rememberNavController()

        // Provide LocalSettingsRepository (device-local storage) to AuthViewModel.
        // Settings() uses multiplatform-settings-no-arg which resolves to
        // SharedPreferences on Android and NSUserDefaults on iOS — no Context needed.
        val localSettings = remember { LocalSettingsRepository(Settings()) }
        val authViewModel: AuthViewModel = viewModel {
            AuthViewModel(localSettings)
        }
        val propertyViewModel: PropertyViewModel = viewModel()
        val tenantViewModel: TenantViewModel = viewModel()

        val authState by authViewModel.authState.collectAsState()
        val sessionChecked by authViewModel.sessionChecked.collectAsState()
        val rememberMeActive by authViewModel.rememberMeActive.collectAsState()
        val selectedRole by authViewModel.selectedRole.collectAsState()
        val propertyListState by propertyViewModel.landlordProperties.collectAsState()
        val tenantPropertyState by propertyViewModel.tenantProperties.collectAsState()
        val formState by propertyViewModel.formState.collectAsState()
        val searchQuery by propertyViewModel.searchQuery.collectAsState()
        val selectedCity by propertyViewModel.selectedCity.collectAsState()
        val unlockedIds by tenantViewModel.unlockedPropertyIds.collectAsState()
        val unlockedProps by tenantViewModel.unlockedProperties.collectAsState()
        val unlockState by tenantViewModel.unlockState.collectAsState()
        val transactions by tenantViewModel.transactions.collectAsState()
        val propertyDraft by propertyViewModel.draft.collectAsState()
        val propertyFilter by propertyViewModel.propertyFilter.collectAsState()

        // Current logged-in user
        val currentUser = (authState as? AuthState.Success)?.user

        // Wait for session check before rendering navigation — prevents a
        // flash of IntroScreen for users with an active rememberMe session.
        if (!sessionChecked) {
            SplashScreen(
                currentUserRole = null,
                onNavigateToRole = {},
                onNavigateToLandlord = {},
                onNavigateToTenant = {}
            )
            return@RentOutTheme
        }

        NavHost(
            navController = navController,
            startDestination = NavRoutes.INTRO,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            // -- INTRO ---------------------------------------------------------
            composable(NavRoutes.INTRO) {
                IntroScreen(
                    onGetStarted = {
                        if (rememberMeActive) {
                            // Short path: Intro ? Splash ? Dashboard
                            navController.navigate(NavRoutes.SPLASH) {
                                popUpTo(NavRoutes.INTRO) { inclusive = false }
                            }
                        } else {
                            // Normal path: Intro ? Role Selection ? Auth ? Splash ? Dashboard
                            navController.navigate(NavRoutes.ROLE_SELECTION)
                        }
                    }
                )
            }

            // -- ROLE SELECTION ------------------------------------------------
            composable(NavRoutes.ROLE_SELECTION) {
                RoleSelectionScreen(
                    onRoleSelected = { role ->
                        authViewModel.selectRole(role)
                        navController.navigate("auth?prefillEmail=&prefillPassword=")
                    }
                )
            }

            // -- AUTH ----------------------------------------------------------
            composable(
                route = NavRoutes.AUTH,
                arguments = listOf(
                    navArgument("prefillEmail")    { type = NavType.StringType; defaultValue = "" },
                    navArgument("prefillPassword") { type = NavType.StringType; defaultValue = "" }
                )
            ) {
                // When arriving after registration, pre-fill login fields
                val prefillEmail    = it.arguments?.getString("prefillEmail")    ?: ""
                val prefillPassword = it.arguments?.getString("prefillPassword") ?: ""
                val initialTab      = 0  // always start on Login tab

                val registrationProgress by authViewModel.registrationProgress.collectAsState()
                val registrationStep     by authViewModel.registrationStep.collectAsState()

                AuthScreen(
                    selectedRole = selectedRole,
                    authState = authState,
                    onLogin = { email, password, rememberMe ->
                        authViewModel.onEvent(AuthEvent.Login(email, password, rememberMe))
                    },
                    onRegister = { name, email, password, phoneNumber, profilePhotoUrl, photoBytes ->
                        authViewModel.onEvent(AuthEvent.Register(name, email, password, selectedRole, phoneNumber, profilePhotoUrl, photoBytes))
                    },
                    onBack = { navController.popBackStack() },
                    onClearError = { authViewModel.clearError() },
                    prefillEmail = prefillEmail,
                    prefillPassword = prefillPassword,
                    initialTab = initialTab,
                    registrationProgress = registrationProgress,
                    registrationStep = registrationStep
                )
                // React to auth state
                LaunchedEffect(authState) {
                    when (val state = authState) {
                        is AuthState.Registered -> {
                            // Account created — go back to login tab with credentials pre-filled
                            authViewModel.clearRegistered()
                            navController.navigate(
                                NavRoutes.authWithPrefill(state.email, state.password)
                            ) {
                                popUpTo(NavRoutes.AUTH) { inclusive = true }
                            }
                        }
                        is AuthState.Success -> {
                            navController.navigate(NavRoutes.SPLASH) {
                                popUpTo(NavRoutes.INTRO) { inclusive = true }
                            }
                        }
                        is AuthState.Suspended -> {
                            navController.navigate(NavRoutes.SUSPENDED)
                        }
                        else -> {}
                    }
                }
            }

            // -- SPLASH --------------------------------------------------------
            composable(NavRoutes.SPLASH) {
                // Preload full user profile (including profilePhotoUrl) during the
                // splash animation window. By the time the dashboard renders, the
                // authState already contains the correct profilePhotoUrl and Coil
                // will have had time to begin fetching the image into its cache.
                LaunchedEffect(Unit) { authViewModel.refreshUser() }
                val user = currentUser
                SplashScreen(
                    currentUserRole = user?.role,
                    onNavigateToRole = {
                        navController.navigate(NavRoutes.ROLE_SELECTION) {
                            popUpTo(NavRoutes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToLandlord = {
                        // Load from Firestore
                        user?.let { propertyViewModel.loadLandlordPropertiesFromFirestore(it.uid) }
                        navController.navigate(NavRoutes.LANDLORD_DASHBOARD) {
                            popUpTo(NavRoutes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToTenant = {
                        // Load from Firestore
                        propertyViewModel.loadTenantPropertiesFromFirestore()
                        navController.navigate(NavRoutes.TENANT_HOME) {
                            popUpTo(NavRoutes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            // -- SUSPENDED -----------------------------------------------------
            composable(NavRoutes.SUSPENDED) {
                SuspendedScreen(
                    onContactSupport = {},
                    onLogout = {
                        authViewModel.onEvent(AuthEvent.Logout)
                        navController.navigate(NavRoutes.INTRO) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // -- LANDLORD DASHBOARD --------------------------------------------
            composable(NavRoutes.LANDLORD_DASHBOARD) {
                // Ensure the real-time listener is active whenever this screen
                // is (re)entered — covers cold start, back-navigation, and
                // returning from a sub-screen after logout/re-login.
                LaunchedEffect(currentUser?.uid) {
                    currentUser?.uid?.let { uid ->
                        propertyViewModel.loadLandlordPropertiesFromFirestore(uid)
                    }
                }
                LandlordDashboardScreen(
                    user = currentUser ?: User(),
                    propertyListState = propertyListState,
                    onAddProperty = { navController.navigate(NavRoutes.ADD_PROPERTY) },
                    onPropertyClick = { property ->
                        propertyViewModel.selectProperty(property)
                        navController.navigate(NavRoutes.landlordPropertyDetail(property.id))
                    },
                    onEditProperty = { property ->
                        propertyViewModel.selectProperty(property)
                        navController.navigate(NavRoutes.editProperty(property.id))
                    },
                    onDeleteProperty = { propertyViewModel.deleteProperty(it) },
                    onToggleAvailability = { propertyViewModel.toggleAvailability(it) },
                    onProfileClick = { navController.navigate(NavRoutes.LANDLORD_PROFILE) },
                    onLogout = {
                        propertyViewModel.stopAllListeners()
                        authViewModel.onEvent(AuthEvent.Logout)
                        navController.navigate(NavRoutes.INTRO) { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            // -- LANDLORD PROPERTY DETAIL --------------------------------------
            composable(
                route = NavRoutes.LANDLORD_PROPERTY_DETAIL,
                arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
            ) {
                val property = propertyViewModel.selectedProperty.collectAsState().value
                if (property != null) {
                    LandlordPropertyDetailScreen(
                        property = property,
                        onBack = { navController.popBackStack() },
                        onEdit = {
                            navController.navigate(NavRoutes.editProperty(property.id))
                        },
                        onToggleAvailability = {
                            propertyViewModel.toggleAvailability(property.id)
                            navController.popBackStack()
                        },
                        onDelete = {
                            propertyViewModel.deleteProperty(property.id)
                            navController.popBackStack()
                        }
                    )
                }
            }

            // -- EDIT PROPERTY IMAGES ------------------------------------------
            composable(
                route = NavRoutes.EDIT_PROPERTY_IMAGES,
                arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
            ) {
                val property = propertyViewModel.selectedProperty.collectAsState().value
                if (property != null) {
                    LaunchedEffect(formState) {
                        if (formState is PropertyFormState.Success) {
                            propertyViewModel.resetFormState()
                            navController.popBackStack()
                        }
                    }
                    EditPropertyImagesScreen(
                        property  = property,
                        formState = formState,
                        viewModel = propertyViewModel,
                        onSave    = { keepUrls, newBytes ->
                            // Use the LATEST property from selectedProperty state, not the captured one
                            val latestProperty = propertyViewModel.selectedProperty.value
                            if (latestProperty != null) {
                                propertyViewModel.updateProperty(
                                    property      = latestProperty,
                                    keepImageUrls = keepUrls,
                                    newImageBytes = newBytes
                                )
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // -- LANDLORD PROFILE ----------------------------------------------
            composable(NavRoutes.LANDLORD_PROFILE) {
                LandlordProfileScreen(
                    user = currentUser ?: User(),
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        propertyViewModel.stopAllListeners()
                        authViewModel.onEvent(AuthEvent.Logout)
                        navController.navigate(NavRoutes.INTRO) { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            // -- ADD PROPERTY --------------------------------------------------
            composable(NavRoutes.ADD_PROPERTY) {
                // Navigate back to dashboard and refresh when submission succeeds
                LaunchedEffect(formState) {
                    if (formState is PropertyFormState.Success) {
                        propertyViewModel.clearDraft()
                        propertyViewModel.resetFormState()
                        navController.navigate(NavRoutes.LANDLORD_DASHBOARD) {
                            popUpTo(NavRoutes.LANDLORD_DASHBOARD) { inclusive = false }
                        }
                    }
                }
                AddPropertyScreen(
                    formState = formState,
                    onSubmit = { property: org.example.project.data.model.Property ->
                        propertyViewModel.submitProperty(property, emptyList())
                    },
                    onBack = {
                        // Clear the draft when the landlord explicitly goes back
                        // (abandons the form), so it doesn't bleed into a future listing.
                        propertyViewModel.clearDraft()
                        navController.popBackStack()
                    },
                    onNavigateToImages = { property: org.example.project.data.model.Property ->
                        propertyViewModel.selectProperty(property)
                        navController.navigate(NavRoutes.PROPERTY_IMAGES)
                    },
                    landlordPhoneNumber = currentUser?.phoneNumber ?: "",
                    draft = propertyDraft,
                    onSaveDraft = { propertyViewModel.saveDraft(it) }
                )
            }

            // -- PROPERTY IMAGES -----------------------------------------------
            composable(NavRoutes.PROPERTY_IMAGES) {
                val property = propertyViewModel.selectedProperty.collectAsState().value
                // Navigate back to dashboard and refresh when submission succeeds
                LaunchedEffect(formState) {
                    if (formState is PropertyFormState.Success) {
                        propertyViewModel.clearDraft()
                        propertyViewModel.resetFormState()
                        navController.navigate(NavRoutes.LANDLORD_DASHBOARD) {
                            popUpTo(NavRoutes.LANDLORD_DASHBOARD) { inclusive = false }
                        }
                    }
                }
                if (property != null) {
                    PropertyImagesScreen(
                        property  = property,
                        formState = formState,
                        viewModel = propertyViewModel,
                        onSubmit  = { prop: org.example.project.data.model.Property, imageBytes: List<ByteArray> ->
                            propertyViewModel.submitProperty(prop, imageBytes)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // -- EDIT PROPERTY -------------------------------------------------
            composable(NavRoutes.EDIT_PROPERTY) {
                val property = propertyViewModel.selectedProperty.collectAsState().value
                if (property != null) {
                    // Navigate back to dashboard on successful update
                    LaunchedEffect(formState) {
                        if (formState is PropertyFormState.Success) {
                            propertyViewModel.resetFormState()
                            navController.navigate(NavRoutes.LANDLORD_DASHBOARD) {
                                popUpTo(NavRoutes.LANDLORD_DASHBOARD) { inclusive = false }
                            }
                        }
                    }
                    // Seed the draft from the existing property so all fields are pre-filled
                    val editDraft = remember(property.id) {
                        // Parse address back into components (stored as "street, suburb, city, country")
                        val parts = property.location.split(", ")
                        PropertyDraft(
                            title           = property.title
                                .replace(" in ${property.city}", "")
                                .let { t ->
                                    // Strip suburb suffix: "Title in SuburbName" → "Title"
                                    val suburbPart = if (parts.size >= 2) parts.getOrNull(1) ?: "" else ""
                                    if (suburbPart.isNotBlank() && t.endsWith(" in $suburbPart"))
                                        t.removeSuffix(" in $suburbPart") else t
                                },
                            price           = property.price.let {
                                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                            },
                            securityDeposit = property.securityDeposit.let {
                                if (it == 0.0) "" else if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                            },
                            rooms           = property.rooms.toString(),
                            bathrooms       = property.bathrooms.toString(),
                            description     = property.description,
                            propType        = property.propertyType,
                            houseAndStreet  = parts.getOrNull(0) ?: "",
                            suburb          = parts.getOrNull(1) ?: "",
                            townOrCity      = parts.getOrNull(2) ?: property.city,
                            country         = parts.getOrNull(3) ?: "Zimbabwe",
                            contact         = property.contactNumber,
                            amenityKeys     = property.amenities.toSet()
                        )
                    }
                    // Collect ALL uploaded images — prefer imageUrls list, fall back to imageUrl
                    val existingImages = remember(property.id) {
                        property.imageUrls.ifEmpty {
                            listOfNotNull(property.imageUrl.takeIf { it.isNotBlank() })
                        }
                    }
                    AddPropertyScreen(
                        formState            = formState,
                        onSubmit             = { updated: org.example.project.data.model.Property ->
                            // Preserve existing metadata when updating
                            propertyViewModel.updateProperty(
                                updated.copy(
                                    id           = property.id,
                                    landlordId   = property.landlordId,
                                    landlordName = property.landlordName,
                                    imageUrl     = property.imageUrl,
                                    isVerified   = property.isVerified,
                                    createdAt    = property.createdAt,
                                    status       = property.status
                                )
                            )
                        },
                        onBack               = { navController.popBackStack() },
                        onNavigateToImages   = { prop: org.example.project.data.model.Property ->
                            propertyViewModel.selectProperty(
                                prop.copy(
                                    id           = property.id,
                                    landlordId   = property.landlordId,
                                    landlordName = property.landlordName,
                                    imageUrl     = property.imageUrl,
                                    imageUrls    = property.imageUrls,
                                    isVerified   = property.isVerified,
                                    createdAt    = property.createdAt,
                                    status       = property.status
                                )
                            )
                            navController.navigate(NavRoutes.editPropertyImages(property.id))
                        },
                        landlordPhoneNumber  = currentUser?.phoneNumber ?: "",
                        draft                = editDraft,
                        onSaveDraft          = { propertyViewModel.saveDraft(it) },
                        isEditMode           = true,
                        existingImageUrls    = existingImages
                    )
                }
            }

            // -- TENANT HOME ---------------------------------------------------
            composable(NavRoutes.TENANT_HOME) {
                // Start/resume real-time listeners whenever this screen is active.
                // Using currentUser?.uid as the key means if the user changes
                // (logout + new login) the listeners are restarted for the new user.
                LaunchedEffect(currentUser?.uid) {
                    propertyViewModel.loadTenantPropertiesFromFirestore()
                    currentUser?.uid?.let { uid ->
                        tenantViewModel.loadUnlockedProperties(uid)
                        tenantViewModel.loadTransactions(uid)
                    }
                }
                TenantHomeScreen(
                    user = currentUser ?: User(),
                    propertyListState = tenantPropertyState,
                    searchQuery = searchQuery,
                    selectedCity = selectedCity,
                    unlockedPropertyIds = unlockedIds,
                    activeFilter = propertyFilter,
                    onSearchQueryChange = { propertyViewModel.setSearchQuery(it) },
                    onCityChange = { propertyViewModel.setSelectedCity(it) },
                    onFilterChange = { propertyViewModel.setFilter(it) },
                    onClearFilter = { propertyViewModel.clearFilter() },
                    onPropertyClick = { property ->
                        propertyViewModel.selectProperty(property)
                        navController.navigate(NavRoutes.propertyDetail(property.id))
                    },
                    onUnlockedClick = { navController.navigate(NavRoutes.UNLOCKED_PROPERTIES) },
                    onProfileClick = { navController.navigate(NavRoutes.TENANT_PROFILE) },
                    onLogout = {
                        propertyViewModel.stopAllListeners()
                        tenantViewModel.stopListeners()
                        authViewModel.onEvent(AuthEvent.Logout)
                        navController.navigate(NavRoutes.INTRO) { popUpTo(0) { inclusive = true } }
                    },
                    applyFilters = { props, query, city, filter ->
                        propertyViewModel.applyFilters(props, query, city, filter)
                    }
                )
            }

            // -- PROPERTY DETAIL -----------------------------------------------
            composable(NavRoutes.PROPERTY_DETAIL) {
                val property = propertyViewModel.selectedProperty.collectAsState().value
                if (property != null) {
                    val isUnlocked = tenantViewModel.isPropertyUnlocked(property.id)
                    PropertyDetailScreen(
                        property = property,
                        isUnlocked = isUnlocked,
                        onUnlock = { navController.navigate(NavRoutes.payment(property.id)) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // -- PAYMENT -------------------------------------------------------
            composable(NavRoutes.PAYMENT) {
                val property = propertyViewModel.selectedProperty.collectAsState().value
                if (property != null) {
                    PaymentScreen(
                        property = property,
                        unlockState = unlockState,
                        onPay = {
                            currentUser?.let { user ->
                                tenantViewModel.initiateAndConfirmPayment(user.uid, property)
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onSuccess = {
                            tenantViewModel.resetUnlockState()
                            navController.navigate(NavRoutes.propertyDetail(property.id)) {
                                popUpTo(NavRoutes.TENANT_HOME)
                            }
                        }
                    )
                }
            }

            // -- UNLOCKED PROPERTIES -------------------------------------------
            composable(NavRoutes.UNLOCKED_PROPERTIES) {
                UnlockedPropertiesScreen(
                    unlockedProperties = unlockedProps,
                    onPropertyClick = { property ->
                        propertyViewModel.selectProperty(property)
                        navController.navigate(NavRoutes.propertyDetail(property.id))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // -- TENANT PROFILE ------------------------------------------------
            composable(NavRoutes.TENANT_PROFILE) {
                // Ensure transactions are loaded when navigating to profile screen
                LaunchedEffect(currentUser?.uid) {
                    currentUser?.uid?.let { uid ->
                        println("🔄 TENANT_PROFILE: Starting transaction listener for uid=$uid")
                        tenantViewModel.loadTransactions(uid)
                    }
                }
                TenantProfileScreen(
                    user = currentUser ?: User(),
                    unlockedCount = unlockedProps.size,
                    transactions = transactions,
                    onUnlockedClick = { navController.navigate(NavRoutes.UNLOCKED_PROPERTIES) },
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        propertyViewModel.stopAllListeners()
                        tenantViewModel.stopListeners()
                        authViewModel.onEvent(AuthEvent.Logout)
                        navController.navigate(NavRoutes.INTRO) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
        }
    }
}
