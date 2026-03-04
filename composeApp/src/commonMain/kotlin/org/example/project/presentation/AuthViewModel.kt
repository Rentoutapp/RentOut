package org.example.project.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.model.User

// ─── UI State ─────────────────────────────────────────────────────────────────
sealed class AuthState {
    object Idle      : AuthState()
    object Loading   : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
    object Suspended : AuthState()
}

sealed class AuthEvent {
    data class Login(val email: String, val password: String) : AuthEvent()
    data class Register(val name: String, val email: String, val password: String, val role: String) : AuthEvent()
    object Logout : AuthEvent()
}

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _selectedRole = MutableStateFlow("")
    val selectedRole: StateFlow<String> = _selectedRole.asStateFlow()

    fun selectRole(role: String) {
        _selectedRole.value = role
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.Login    -> login(event.email, event.password)
            is AuthEvent.Register -> register(event.name, event.email, event.password, event.role)
            is AuthEvent.Logout   -> logout()
        }
    }

    private fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Firebase auth login — stubbed for MVP scaffold; wire Firebase here
                // val user = firebaseAuthRepository.login(email, password)
                // Simulate for now with demo credentials
                val demoUser = when (email.lowercase().trim()) {
                    "landlord@rentout.demo" -> User(uid = "demo_landlord", name = "Demo Landlord", email = email, role = "landlord")
                    "tenant@rentout.demo"   -> User(uid = "demo_tenant",   name = "Demo Tenant",   email = email, role = "tenant")
                    "admin@rentout.demo"    -> User(uid = "demo_admin",    name = "Demo Admin",    email = email, role = "admin")
                    else -> null
                }
                if (demoUser != null && password == "demo1234!") {
                    _authState.value = AuthState.Success(demoUser)
                } else {
                    _authState.value = AuthState.Error("Invalid email or password.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed. Please try again.")
            }
        }
    }

    private fun register(name: String, email: String, password: String, role: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Firebase auth register — wire Firebase here
                if (name.isBlank() || email.isBlank() || password.length < 6) {
                    _authState.value = AuthState.Error("Please fill all fields correctly.")
                    return@launch
                }
                val newUser = User(uid = "new_${System.currentTimeMillis()}", name = name, email = email, role = role)
                _authState.value = AuthState.Success(newUser)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed. Please try again.")
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.Idle
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}
