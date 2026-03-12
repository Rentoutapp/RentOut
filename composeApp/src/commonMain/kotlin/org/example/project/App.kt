package org.example.project

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import org.example.project.ui.screens.NotificationScreen
import org.example.project.ui.navigation.NavRoutes
import org.example.project.ui.screens.auth.*
import org.example.project.ui.screens.landlord.*
import org.example.project.ui.screens.landlord.ButtonAnimationDemoScreen
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
        val brokerageViewModel: BrokerageViewModel = viewModel()
        val notificationViewModel: NotificationViewModel = viewModel()

        val authState by authViewModel.authState.collectAsState()
        val sessionChecked by authViewModel.sessionChecked.collectAsState()
        val rememberMeActive by authViewModel.rememberMeActive.collectAsState()
        val selectedRole by authViewModel.selectedRole.collectAsState()
        val selectedSubtype by authViewModel.selectedSubtype.collectAsState()
        val propertyListState by propertyViewModel.landlordProperties.collectAsState()
        val tenantPropertyState by propertyViewModel.tenantProperties.collectAsState()
        val formState by propertyViewModel.formState.collectAsState()
        val searchQuery by propertyViewModel.searchQuery.collectAsState()
        val selectedCity by propertyViewModel.selectedCity.collectAsState()
        val unlockedIds by tenantViewModel.unlockedPropertyIds.collectAsState()
        val unlockedProps by tenantViewModel.unlockedProperties.collectAsState()
        val unlockState by tenantViewModel.unlockState.collectAsState()
        val transactions by tenantViewModel.transactions.collectAsState()
        val transactionsLoading by tenantViewModel.transactionsLoading.collectAsState()
        val brokerageAccount by brokerageViewModel.brokerageAccount.collectAsState()
        val brokerageLedger by brokerageViewModel.ledgerEntries.collectAsState()
        val brokerageAccountLoading by brokerageViewModel.isAccountLoading.collectAsState()
        val brokerageLedgerLoading by brokerageViewModel.isLedgerLoading.collectAsState()
        val brokerageTopUpState by brokerageViewModel.topUpState.collectAsState()
        val propertyDraft by propertyViewModel.draft.collectAsState()
        val propertyFilter by propertyViewModel.propertyFilter.collectAsState()
        val notificationState by notificationViewModel.state.collectAsState()
        val unreadNotificationCount by notificationViewModel.unreadCount.collectAsState()

        // Current logged-in user
        val currentUser = (authState as? AuthState.Success)?.user

        // Start/restart notification listener whenever the logged-in user changes.
        // This covers: fresh login, remember-me session restore, and back-navigation.
        LaunchedEffect(currentUser?.uid) {
            val uid = currentUser?.uid
            if (uid != null) {
                notificationViewModel.startListening(uid)
            } else {
                notificationViewModel.stopListening()
            }
        }

        // During session check (remember-me path), Firebase auth currentUser is
        // available before authState.Success is emitted. Start the notification
        // listener as soon as we have a UID from any source so the bell badge
        // and notification list are ready the moment the dashboard appears.
        LaunchedEffect(sessionChecked, authState) {
            if (sessionChecked) {
                val uid = (authState as? AuthState.Success)?.user?.uid
                if (uid != null && notificationViewModel.currentListeningUid != uid) {
                    notificationViewModel.startListening(uid)
                }
            }
        }

        // While the session check is in-flight, show a plain black screen —
        // no text, no logo, no flash before the intro video starts.
        if (!sessionChecked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
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
            // Plays intro_vid.mp4 full-screen. After video ends, always goes to
            // Role Selection — no button, no rememberMe branching here.
            composable(NavRoutes.INTRO) {
                IntroScreen(
                    rememberMeActive = rememberMeActive,
                    onGetStarted = {
                        navController.navigate(NavRoutes.ROLE_SELECTION) {
                            popUpTo(NavRoutes.INTRO) { inclusive = true }
                        }
                    },
                    onAutoLogin = {
                        // Remember Me active — skip Role Selection & Auth entirely,
                        // go straight to Splash which routes to the correct dashboard.
                        navController.navigate(NavRoutes.SPLASH) {
                            popUpTo(NavRoutes.INTRO) { inclusive = true }
                        }
                    }
                )
            }

            // -- ROLE SELECTION ------------------------------------------------
            composable(NavRoutes.ROLE_SELECTION) {
                RoleSelectionScreen(
                    onRoleSelected = { role, subtype ->
                        authViewModel.selectRole(role)
                        authViewModel.selectSubtype(subtype)
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
                    selectedSubtype = selectedSubtype,
                    authState = authState,
                    onLogin = { email, password, rememberMe ->
                        authViewModel.onEvent(AuthEvent.Login(
                            email           = email,
                            password        = password,
                            rememberMe      = rememberMe,
                            expectedRole    = selectedRole,
                            expectedSubtype = selectedSubtype
                        ))
                    },
                    onNavigateAfterLogin = {
                        navController.navigate(NavRoutes.SPLASH) {
                            popUpTo(NavRoutes.INTRO) { inclusive = true }
                        }
                    },
                    onRegister = { name, email, password, phoneNumber, profilePhotoUrl, photoBytes, gender, nationalId,
                                   providerSubtype, agentLicenseNumber, yearsOfExperience,
                                   companyName, companyRegNumber,
                                   companyStreet, companyCity, companyCountry,
                                   taxId, companyPhone, companyEmail, companyLogoUrl, logoBytes ->
                        authViewModel.onEvent(AuthEvent.Register(
                            name = name, email = email, password = password,
                            role = selectedRole, providerSubtype = providerSubtype,
                            phoneNumber = phoneNumber, profilePhotoUrl = profilePhotoUrl,
                            photoBytes = photoBytes, gender = gender, nationalId = nationalId,
                            agentLicenseNumber = agentLicenseNumber, yearsOfExperience = yearsOfExperience,
                            companyName = companyName, companyRegNumber = companyRegNumber,
                            companyStreet = companyStreet, companyCity = companyCity,
                            companyCountry = companyCountry, taxId = taxId,
                            companyPhone = companyPhone, companyEmail = companyEmail,
                            companyLogoUrl = companyLogoUrl, logoBytes = logoBytes
                        ))
                    },
                    onBack = { navController.popBackStack() },
                    onClearError = { authViewModel.clearError() },
                    prefillEmail = prefillEmail,
                    prefillPassword = prefillPassword,
                    initialTab = initialTab,
                    registrationProgress = registrationProgress,
                    registrationStep = registrationStep
                )
                // React to auth state (registration and suspension only — login navigation
                // is handled inside AuthScreen after the progress bar completes)
                LaunchedEffect(authState) {
                    when (val state = authState) {
                        is AuthState.Registered -> {
                            // Account created – go back to login tab with credentials pre-filled
                            authViewModel.clearRegistered()
                            navController.navigate(
                                NavRoutes.authWithPrefill(state.email, state.password)
                            ) {
                                popUpTo(NavRoutes.AUTH) { inclusive = true }
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
                // Refresh the user profile from Firestore so all fields (role,
                // brokerage balance, etc.) are up-to-date before the dashboard
                // renders. We also eagerly start the notification listener here
                // so the bell badge and notification list are ready on arrival.
                LaunchedEffect(Unit) {
                    authViewModel.refreshUser()
                }
                // Once authState.Success is available (either from refreshUser()
                // or from the remember-me session check), start the notification
                // listener immediately so past events are loaded during the
                // splash animation window rather than after dashboard navigation.
                LaunchedEffect(authState) {
                    val uid = (authState as? AuthState.Success)?.user?.uid
                    if (uid != null) {
                        notificationViewModel.startListening(uid)
                    }
                }
                val user = currentUser
                SplashScreen(
                    currentUserRole = user?.role,
                    onNavigateToRole = {
                        navController.navigate(NavRoutes.ROLE_SELECTION) {
                            popUpTo(NavRoutes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToLandlord = {
                        user?.let { propertyViewModel.loadLandlordPropertiesFromFirestore(it.uid) }
                        navController.navigate(NavRoutes.LANDLORD_DASHBOARD) {
                            popUpTo(NavRoutes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToTenant = {
                        propertyViewModel.resetTenantBrowseState()
                        propertyViewModel.loadTenantPropertiesFromFirestore()
                        navController.navigate(NavRoutes.TENANT_HOME) {
                            popUpTo(NavRoutes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            // -- NOTIFICATIONS -------------------------------------------------
            composable(NavRoutes.NOTIFICATIONS) {
                // Ensure the notification listener is running whenever this screen
                // is entered — covers the case where the user navigated here from
                // any dashboard without a UID change triggering the top-level effect.
                LaunchedEffect(currentUser?.uid) {
                    val uid = currentUser?.uid
                    if (uid != null) {
                        notificationViewModel.startListening(uid)
                    }
                }
                NotificationScreen(
                    state        = notificationState,
                    unreadCount  = unreadNotificationCount,
                    onBack       = { navController.popBackStack() },
                    onMarkAllRead = { notificationViewModel.markAllAsRead() },
                    onMarkRead   = { id -> notificationViewModel.markAsRead(id) },
                    onDelete     = { id -> notificationViewModel.deleteNotification(id) }
                )
            }

            // -- SUSPENDED -----------------------------------------------------
            composable(NavRoutes.SUSPENDED) {
                SuspendedScreen(
                    onContactSupport = {},
                    onLogout = {
                        authViewModel.onEvent(AuthEvent.Logout)
                        navController.navigate(NavRoutes.ROLE_SELECTION) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // -- LANDLORD DASHBOARD --------------------------------------------
            composable(NavRoutes.LANDLORD_DASHBOARD) {
                val dashboardUser = if (currentUser?.providerSubtype == "brokerage" && brokerageAccount.uid.isNotBlank()) {
                    brokerageAccount
                } else {
                    currentUser ?: User()
                }
                // Ensure the real-time listener is active whenever this screen
                // is (re)entered — covers cold start, back-navigation, and
                // returning from a sub-screen after logout/re-login.
                LaunchedEffect(currentUser?.uid) {
                    currentUser?.uid?.let { uid ->
                        propertyViewModel.loadLandlordPropertiesFromFirestore(uid)
                        if (currentUser?.providerSubtype == "brokerage") {
                            brokerageViewModel.observeBrokerageAccount(uid)
                            brokerageViewModel.observeLedger(uid)
                        }
                    }
                }
                LandlordDashboardScreen(
                    user = dashboardUser,
                    propertyListState = propertyListState,
                    notificationCount = unreadNotificationCount,
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
                    onNotificationsClick = { navController.navigate(NavRoutes.NOTIFICATIONS) },
                    onProfileClick = { navController.navigate(NavRoutes.LANDLORD_PROFILE) },
                    onBrokerageAccountClick = { navController.navigate(NavRoutes.BROKERAGE_ACCOUNT) },
                    onLogout = {
                        propertyViewModel.stopAllListeners()
                        brokerageViewModel.stop()
                        notificationViewModel.stopListening()
                        authViewModel.onEvent(AuthEvent.Logout)
                        navController.navigate(NavRoutes.ROLE_SELECTION) { popUpTo(0) { inclusive = true } }
                    },
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
                val profileUser = if (currentUser?.providerSubtype == "brokerage" && brokerageAccount.uid.isNotBlank()) brokerageAccount else currentUser ?: User()
                LandlordProfileScreen(
                    user = profileUser,
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        propertyViewModel.stopAllListeners()
                        brokerageViewModel.stop()
                        notificationViewModel.stopListening()
                        authViewModel.onEvent(AuthEvent.Logout)
                        navController.navigate(NavRoutes.ROLE_SELECTION) { popUpTo(0) { inclusive = true } }
                    },
                    onOpenBrokerageAccount = { navController.navigate(NavRoutes.BROKERAGE_ACCOUNT) },
                    onDeleteAccount = {
                        authViewModel.deleteAccount(
                            onSuccess = {
                                propertyViewModel.stopAllListeners()
                                brokerageViewModel.stop()
                                notificationViewModel.stopListening()
                                navController.navigate(NavRoutes.INTRO) { popUpTo(0) { inclusive = true } }
                            },
                            onError = { error ->
                                println("❌ Delete account error: $error")
                            }
                        )
                    }
                )
            }

            composable(NavRoutes.BROKERAGE_ACCOUNT) {
                BrokerageAccountScreen(
                    user = if (brokerageAccount.uid.isNotBlank()) brokerageAccount else currentUser ?: User(),
                    ledgerEntries = brokerageLedger,
                    isLoading = brokerageAccountLoading || brokerageLedgerLoading,
                    topUpState = brokerageTopUpState,
                    onTopUp = { brokerageViewModel.topUpFloat(it) },
                    onOpenHistory = { navController.navigate(NavRoutes.BROKERAGE_PAYMENT_HISTORY) },
                    onBack = { navController.popBackStack() },
                    onResetTopUpState = { brokerageViewModel.resetTopUpState() }
                )
            }

            composable(NavRoutes.BROKERAGE_PAYMENT_HISTORY) {
                BrokeragePaymentHistoryScreen(
                    user = if (brokerageAccount.uid.isNotBlank()) brokerageAccount else currentUser ?: User(),
                    ledgerEntries = brokerageLedger,
                    isLoading = brokerageLedgerLoading,
                    onBack = { navController.popBackStack() }
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
                    landlordPhoneNumber     = currentUser?.phoneNumber ?: "",
                    landlordName            = currentUser?.name ?: "",
                    landlordCompanyName     = currentUser?.companyName ?: "",
                    landlordCompanyPhone    = currentUser?.companyPhone ?: "",
                    landlordCompanyEmail    = currentUser?.companyEmail ?: "",
                    landlordCompanyAddress  = listOfNotNull(
                        currentUser?.companyStreet?.takeIf { it.isNotBlank() },
                        currentUser?.companyCity?.takeIf { it.isNotBlank() }
                    ).joinToString(", "),
                    providerSubtype         = currentUser?.providerSubtype ?: "landlord",
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
                            // ── Basic info ────────────────────────────────────────────────────
                            title                = property.title
                                .replace(" in ${property.city}", "")
                                .let { t ->
                                    val suburbPart = if (parts.size >= 2) parts.getOrNull(1) ?: "" else ""
                                    if (suburbPart.isNotBlank() && t.endsWith(" in $suburbPart"))
                                        t.removeSuffix(" in $suburbPart") else t
                                },
                            description          = property.description,
                            // ── Classification ────────────────────────────────────────────────
                            classification       = property.classification.ifBlank { "Residential" },
                            propType             = property.propertyType.ifBlank { "Apartment" },
                            locationType         = property.locationType,
                            // ── Pricing ───────────────────────────────────────────────────────
                            price                = property.price.let {
                                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                            },
                            securityDeposit      = property.securityDeposit.let {
                                if (it == 0.0) "" else if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                            },
                            depositNotApplicable = property.depositNotApplicable,
                            // ── Bills ─────────────────────────────────────────────────────────
                            billsInclusive       = property.billsInclusive.toSet(),
                            billsExclusive       = property.billsExclusive.toSet(),
                            // ── Rooms & bathrooms ─────────────────────────────────────────────
                            rooms                = if (property.rooms == 0 && property.customBedroomDetails.isNotBlank())
                                                       "other"
                                                   else
                                                       property.rooms.let { if (it == 0) "" else it.toString() },
                            customBedroomDetails = property.customBedroomDetails,
                            bathrooms            = property.bathrooms.let { if (it == 0) "" else it.toString() },
                            bathroomType         = property.bathroomType,
                            customBathroomDetails = property.customBathroomDetails,
                            // ── Kitchen ───────────────────────────────────────────────────────
                            hasSharedKitchen     = property.hasSharedKitchen,
                            kitchenCount         = property.kitchenCount.let { if (it == 0) "" else it.toString() },
                            // ── Room quantity ─────────────────────────────────────────────────
                            roomQuantity         = property.roomQuantity,
                            // ── Proximity & amenities ─────────────────────────────────────────
                            proximityFacilities  = property.proximityFacilities.toSet(),
                            amenityKeys          = property.amenities.toSet(),
                            // ── Location / GPS ────────────────────────────────────────────────
                            latitude             = if (property.latitude == 0.0) "" else property.latitude.toString(),
                            longitude            = if (property.longitude == 0.0) "" else property.longitude.toString(),
                            // ── Address ───────────────────────────────────────────────────────
                            houseAndStreet       = parts.getOrNull(0) ?: "",
                            suburb               = parts.getOrNull(1) ?: "",
                            townOrCity           = parts.getOrNull(2) ?: property.city,
                            country              = parts.getOrNull(3) ?: "Zimbabwe",
                            // ── Contact ───────────────────────────────────────────────────────
                            contact              = property.contactNumber,
                            // ── Brokerage-specific ────────────────────────────────────────────
                            brokerName             = property.brokerName,
                            brokerContactNumber    = property.brokerContactNumber,
                            brokerageName          = property.brokerageName,
                            brokerageAddress       = property.brokerageAddress,
                            brokerageContactNumber = property.brokerageContactNumber,
                            brokerageEmail         = property.brokerageEmail,
                            // ── Availability & tenant prefs ───────────────────────────────────
                            availabilityDate     = property.availabilityDate,
                            tenantRequirements   = property.tenantRequirements.toSet()
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
                            // Preserve existing images — keepImageUrls holds all current
                            // remote URLs so updateProperty() does not wipe them when
                            // no new images are picked during a metadata-only edit.
                            val currentImages = property.imageUrls.ifEmpty {
                                listOfNotNull(property.imageUrl.takeIf { it.isNotBlank() })
                            }
                            propertyViewModel.updateProperty(
                                property      = updated.copy(
                                    id           = property.id,
                                    landlordId   = property.landlordId,
                                    landlordName = property.landlordName,
                                    imageUrl     = property.imageUrl,
                                    imageUrls    = currentImages,
                                    isVerified   = property.isVerified,
                                    createdAt    = property.createdAt,
                                    status       = property.status
                                ),
                                keepImageUrls = currentImages,
                                newImageBytes = emptyList()
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
                        landlordPhoneNumber     = currentUser?.phoneNumber ?: "",
                        landlordName            = currentUser?.name ?: "",
                        landlordCompanyName     = currentUser?.companyName ?: "",
                        landlordCompanyPhone    = currentUser?.companyPhone ?: "",
                        landlordCompanyEmail    = currentUser?.companyEmail ?: "",
                        landlordCompanyAddress  = listOfNotNull(
                            currentUser?.companyStreet?.takeIf { it.isNotBlank() },
                            currentUser?.companyCity?.takeIf { it.isNotBlank() }
                        ).joinToString(", "),
                        providerSubtype         = currentUser?.providerSubtype ?: "landlord",
                        draft                   = editDraft,
                        onSaveDraft             = { propertyViewModel.saveDraft(it) },
                        isEditMode              = true,
                        existingImageUrls       = existingImages
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
                    transactions = transactions,
                    activeFilter = propertyFilter,
                    notificationCount = unreadNotificationCount,
                    onSearchQueryChange = { propertyViewModel.setSearchQuery(it) },
                    onCityChange = { propertyViewModel.setSelectedCity(it) },
                    onFilterChange = { propertyViewModel.setFilter(it) },
                    onClearFilter = { propertyViewModel.clearFilter() },
                    onPropertyClick = { property ->
                        propertyViewModel.selectProperty(property)
                        navController.navigate(NavRoutes.propertyDetail(property.id))
                    },
                    onUnlockedClick = { navController.navigate(NavRoutes.UNLOCKED_PROPERTIES) },
                    onNotificationsClick = { navController.navigate(NavRoutes.NOTIFICATIONS) },
                    onProfileClick = { navController.navigate(NavRoutes.TENANT_PROFILE) },
                    onLogout = {
                        propertyViewModel.stopAllListeners()
                        tenantViewModel.stopListeners()
                        notificationViewModel.stopListening()
                        authViewModel.onEvent(AuthEvent.Logout)
                        navController.navigate(NavRoutes.ROLE_SELECTION) { popUpTo(0) { inclusive = true } }
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
                    // Collect as state so PropertyDetailScreen recomposes when
                    // syncUnlockStateFromTransactions() updates _unlockedPropertyIds
                    val isUnlocked = unlockedIds.contains(property.id)
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
                        providerSubtype = property.providerSubtype.ifBlank {
                            when {
                                property.brokerName.isNotBlank() || property.brokerageName.isNotBlank() -> "brokerage"
                                property.agentName.isNotBlank() || property.agentContactNumber.isNotBlank() -> "agent"
                                else -> "landlord"
                            }
                        },
                        onPay = {
                            currentUser?.let { user ->
                                tenantViewModel.initiateAndConfirmPayment(user.uid, property)
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onSuccess = {
                            tenantViewModel.resetUnlockState()
                            // Force-refresh unlocks + transactions immediately after
                            // successful payment so dashboard + profile stats update
                            currentUser?.uid?.let { uid ->
                                tenantViewModel.refreshAfterPayment(uid)
                            }
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
                // Ensure both transactions AND unlocks are loaded when navigating
                // to the profile screen — covers cold-start and back-navigation
                LaunchedEffect(currentUser?.uid) {
                    currentUser?.uid?.let { uid ->
                        println("📋 TENANT_PROFILE: Loading transactions + unlocks for uid=$uid")
                        tenantViewModel.loadTransactions(uid)
                        tenantViewModel.loadUnlockedProperties(uid)
                    }
                }
                TenantProfileScreen(
                    user = currentUser ?: User(),
                    unlockedCount = unlockedProps.size,
                    transactions = transactions,
                    transactionsLoading = transactionsLoading,
                    onRefreshTransactions = {
                        currentUser?.uid?.let { uid -> tenantViewModel.refreshTransactions(uid) }
                    },
                    onPaymentHistoryClick = {
                        navController.navigate(NavRoutes.PAYMENT_HISTORY)
                    },
                    onUnlockedClick = { navController.navigate(NavRoutes.UNLOCKED_PROPERTIES) },
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        propertyViewModel.stopAllListeners()
                        tenantViewModel.stopListeners()
                        notificationViewModel.stopListening()
                        authViewModel.onEvent(AuthEvent.Logout)
                        navController.navigate(NavRoutes.ROLE_SELECTION) { popUpTo(0) { inclusive = true } }
                    },
                    onDeleteAccount = {
                        authViewModel.deleteAccount(
                            onSuccess = {
                                propertyViewModel.stopAllListeners()
                                tenantViewModel.stopListeners()
                                notificationViewModel.stopListening()
                                navController.navigate(NavRoutes.INTRO) { popUpTo(0) { inclusive = true } }
                            },
                            onError = { error ->
                                println("❌ Delete account error: $error")
                            }
                        )
                    }
                )
            }

            // -- PAYMENT HISTORY ----------------------------------------------
            composable(NavRoutes.PAYMENT_HISTORY) {
                PaymentHistoryScreen(
                    transactions = transactions,
                    isLoading = transactionsLoading,
                    onRefresh = {
                        currentUser?.uid?.let { uid -> tenantViewModel.refreshTransactions(uid) }
                    },
                    onBack = { navController.popBackStack() },
                    onPropertyImageClick = { transaction ->
                        propertyViewModel.loadPropertyById(transaction.propertyId) { loaded ->
                            if (loaded) {
                                navController.navigate(NavRoutes.propertyDetail(transaction.propertyId))
                            }
                        }
                    }
                )
            }

            // -- BUTTON ANIMATION DEMO -----------------------------------------
            composable(NavRoutes.BUTTON_ANIMATION_DEMO) {
                ButtonAnimationDemoScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
