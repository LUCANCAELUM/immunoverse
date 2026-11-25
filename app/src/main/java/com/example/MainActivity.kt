package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.firebase.AuthScreen
import com.example.core.firebase.attemptAutoSignIn
import com.example.core.firebase.configureAppCheck
import com.example.ui.navigation.ImmunoVerseApp
import com.example.ui.theme.ImmunoVerseTheme
import com.example.ui.viewmodel.ImmunoAiViewModel
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureAppCheck(this, intent)

        setContent {
            ImmunoVerseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
                    var showAuthModal by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()
                    val credentialManager = remember { CredentialManager.create(this@MainActivity) }

                    DisposableEffect(Unit) {
                        val listener = FirebaseAuth.AuthStateListener { auth ->
                            currentUser = auth.currentUser
                        }
                        Firebase.auth.addAuthStateListener(listener)
                        onDispose {
                            Firebase.auth.removeAuthStateListener(listener)
                        }
                    }

                    LaunchedEffect(Unit) {
                        if (Firebase.auth.currentUser == null) {
                            attemptAutoSignIn(
                                context = this@MainActivity,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    currentUser = Firebase.auth.currentUser
                                },
                                onUnauthenticated = {},
                                scope = scope
                            )
                        }
                    }

                    val activeUser = currentUser
                    if (showAuthModal && activeUser == null) {
                        AuthScreen(
                            onAuthSuccess = {
                                currentUser = Firebase.auth.currentUser
                                showAuthModal = false
                            },
                            onContinueAsGuest = {
                                showAuthModal = false
                            }
                        )
                    } else {
                        val sessionKey = activeUser?.uid ?: "guest"
                        key(sessionKey) {
                            val mainVm: MainViewModel = viewModel(key = sessionKey)
                            val aiVm: ImmunoAiViewModel = viewModel(key = "ai_$sessionKey")
                            ImmunoVerseApp(
                                mainVm = mainVm,
                                aiVm = aiVm,
                                isSignedIn = activeUser != null,
                                onRequestSignIn = { showAuthModal = true }
                            )
                        }
                    }
                }
            }
        }
    }
}
