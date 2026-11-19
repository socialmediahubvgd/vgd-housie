package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firestore.FirestoreHousieRepository
import com.example.domain.FounderConfig
import com.example.ui.HousieGameViewModel
import com.example.ui.VgdHousieApp
import com.example.ui.WalletViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.theme.VgdHousieTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            VgdHousieTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val credentialManager = remember(context) { CredentialManager.create(context) }
        val isFounder = FounderConfig.isFounderEmail(user.email, user.isEmailVerified)
        val userEmail = user.email ?: ""
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: userEmail.substringBefore("@").ifBlank { "VGD Player" }

        key(user.uid) {
            val repository = remember(user.uid, isFounder) {
                val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FirestoreHousieRepository(
                    db = db,
                    currentUserId = user.uid,
                    isFounderSession = isFounder,
                    auth = auth
                )
            }
            val walletViewModel: WalletViewModel = viewModel(
                key = "wallet_${user.uid}",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        WalletViewModel(
                            FirestoreHousieRepository(
                                db = db,
                                currentUserId = user.uid,
                                isFounderSession = isFounder,
                                auth = auth
                            )
                        )
                    }
                }
            )
            val gameViewModel: HousieGameViewModel = viewModel(
                key = "game_${user.uid}",
                factory = HousieGameViewModel.Factory(
                    repository = repository,
                    playerDisplayName = displayName
                )
            )
            VgdHousieApp(
                viewModel = walletViewModel,
                gameViewModel = gameViewModel,
                isFounder = isFounder,
                currentUserEmail = userEmail,
                onSignOutClick = {
                    signOutUser(credentialManager, scope)
                }
            )
        }
    }
}
