package org.example.project

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.example.project.data.model.User
import org.example.project.presentation.*
import org.example.project.ui.navigation.NavRoutes
import org.example.project.ui.screens.auth.*
import org.example.project.ui.screens.landlord.*
import org.example.project.ui.screens.tenant.*
import org.example.project.ui.theme.RentOutTheme

@Composable
@Preview
fun App() {
    RentOutTheme {
        val navController = rememberNavController()
        val authViewModel: AuthViewModel = viewModel()
        val propertyViewModel: PropertyViewModel = viewModel()
        val tenantViewModel: TenantViewModel = viewModel()

        val authState by authViewModel.authState.collectAsState()
        val selectedRole by authViewModel.selectedRole.collectAsState()
        val propertyListState by propertyViewModel.landlordProperties.collectAsState()
        val tenantPropertyState by propertyViewModel.tenantProperties.collectAsState()
        val formState by propertyViewModel.formState.collectAsState()
        val searchQuery by propertyViewModel.searchQuery.collectAsState()
        val selectedCity by propertyViewModel.selectedCity.collectAsState()
        val unlockedIds by tenantViewModel.unlockedPropertyIds.collectAsState()
        val unlockedProps by tenantViewModel.unlockedProperties.collectAsState()
        val unlockState by tenantViewModel.unlockState.collectAsState()

        // Current logged-in user
        val currentUser = (authState as? AuthState.Success)?.user

        NavHost(
            navController = navController,
            startDestination = NavRoutes.INTRO,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            // ── INTRO ─────────────────────────────────────────────────────────
            composable(NavRoutes.INTRO) {
                IntroScreen(
                    onGetStarted = { navController.navigate(NavRoutes.ROLE_SELECTION) }
                )
            }

            // ── ROLE SELECTION ────────────────────────────────────────────────
            composable(NavRoutes.ROLE_SELECTION) {
                RoleSelectionScreen(
                    onRoleSelected = { role ->
                        authViewModel.selectRole(role)
                        navController.navigate(NavRoutes.AUTH)
                    }
                )
            }

            // ── AUTH ──────────────────────────────────────────────────────────
            composable(NavRoutes.AUTH) {
                AuthScreen(
                    selectedRole = selectedRole,
                    authState = authState,
                    onLogin = { email, password ->
                        authViewModel.onEvent(AuthEvent.Login(email, password))
                    },
                    onRegister = { name, email, password ->
                        authViewModel.onEvent(AuthEvent.Register(name, email, password, selectedRole))
                    },
                    onBack = { navController.popBackStack() },
                    onClearError = { authViewModel.clearError() }
                )
                // React to auth state
                LaunchedEffect(authState) {
                    when (val state = authState) {
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

            // ── SPLASH ────────────────────────────────────────────────────────
            composable(NavRoutes.SPLASH) {
                val user = currentUser
                SplashScreen(
                    currentUserRole = user?.role,
                    onNavigateToRole = {
                        navController.navigate(NavRoutes.ROLE_SELECTION) {
                            popUpTo(NavRoutes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToLandlord = {
                        user?.let { propertyViewModel.loadLandlordProperties(it.uid) }
                        navController.navigate(NavRoutes.LANDLORD_DASHBOARD) {
                            popUpTo(NavRoutes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToTenant = {
                        propertyViewModel.loadTenantProperties()
                        navController.navigate(NavRoutes.TENANT_HOME) {
                            popUpTo(NavRoutes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            // ── SUSPENDED ─────────────────────────────────────────────────────
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

            // ── LANDLORD DASHBOARD ────────────────────────────────────────────
            composable(NavRoutes.LANDLORD_DASHBOARD) {
                LandlordDashboardScreen(
                    user = currentUser ?: User(),
                    propertyListState = propertyListState,
                    onAddProperty = { navController.navigate(NavRoutes.ADD_PROPERTY) },
                    onEditProperty = { property ->
                        propertyViewModel.selectProperty(property)
                        navController.navigate(NavRoutes.editProperty(property.id))
                    },
                    onDeleteProperty = { propertyViewModel.deleteProperty(it) },
                    onToggleAvailability = { propertyViewModel.toggleAvailability(it) },
                    onLogout = {
                        authViewModel.onEvent(AuthEvent.Logout)
                        navController.navigate(NavRoutes.INTRO) { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            // ── ADD PROPERTY ──────────────────────────────────────────────────
            composable(NavRoutes.ADD_PROPERTY) {
                AddPropertyScreen(
                    formState = formState,
                    onSubmit = { property ->
                        propertyViewModel.submitProperty(property)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // ── EDIT PROPERTY ─────────────────────────────────────────────────
            composable(NavRoutes.EDIT_PROPERTY) {
                val property = propertyViewModel.selectedProperty.collectAsState().value
                if (property != null) {
                    AddPropertyScreen(
                        formState = formState,
                        onSubmit = { updated -> propertyViewModel.submitProperty(updated) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // ── TENANT HOME ───────────────────────────────────────────────────
            composable(NavRoutes.TENANT_HOME) {
                TenantHomeScreen(
                    user = currentUser ?: User(),
                    propertyListState = tenantPropertyState,
                    searchQuery = searchQuery,
                    selectedCity = selectedCity,
                    onSearchQueryChange = { propertyViewModel.setSearchQuery(it) },
                    onCityChange = { propertyViewModel.setSelectedCity(it) },
                    onPropertyClick = { property ->
                        propertyViewModel.selectProperty(property)
                        navController.navigate(NavRoutes.propertyDetail(property.id))
                    },
                    onUnlockedClick = { navController.navigate(NavRoutes.UNLOCKED_PROPERTIES) },
                    onProfileClick = { navController.navigate(NavRoutes.TENANT_PROFILE) },
                    onLogout = {
                        authViewModel.onEvent(AuthEvent.Logout)
                        navController.navigate(NavRoutes.INTRO) { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            // ── PROPERTY DETAIL ───────────────────────────────────────────────
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

            // ── PAYMENT ───────────────────────────────────────────────────────
            composable(NavRoutes.PAYMENT) {
                val property = propertyViewModel.selectedProperty.collectAsState().value
                if (property != null) {
                    PaymentScreen(
                        property = property,
                        unlockState = unlockState,
                        onPay = {
                            currentUser?.let { user ->
                                tenantViewModel.processPaymentSuccess(user.uid, property)
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

            // ── UNLOCKED PROPERTIES ───────────────────────────────────────────
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

            // ── TENANT PROFILE ────────────────────────────────────────────────
            composable(NavRoutes.TENANT_PROFILE) {
                TenantProfileScreen(
                    user = currentUser ?: User(),
                    unlockedCount = unlockedProps.size,
                    onUnlockedClick = { navController.navigate(NavRoutes.UNLOCKED_PROPERTIES) },
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        authViewModel.onEvent(AuthEvent.Logout)
                        navController.navigate(NavRoutes.INTRO) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
        }
    }
}
