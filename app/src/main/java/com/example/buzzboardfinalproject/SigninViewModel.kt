package com.example.buzzboardfinalproject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SigninViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    val email = MutableStateFlow("")
    val password = MutableStateFlow("")
    private val _signInError = MutableStateFlow<String?>(null)
    val signInError: StateFlow<String?> = _signInError

    fun updateEmail(newEmail: String) {
        email.value = newEmail
    }

    fun updatePassword(newPassword: String) {
        password.value = newPassword
    }

    fun onSignInClick(openAndPopUp: () -> Unit) {
        viewModelScope.launch {
            try {
                firebaseAuth.signInWithEmailAndPassword(email.value, password.value)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            openAndPopUp()
                        } else {
                            _signInError.value = task.exception?.localizedMessage ?: "Sign-in failed"
                        }
                    }
            } catch (e: Exception) {
                _signInError.value = e.localizedMessage ?: "Unexpected error"
            }
        }
    }
}

annotation class HiltViewModel
