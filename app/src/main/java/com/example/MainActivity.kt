package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.data.model.Customer
import com.example.data.repository.AiReminderRepository
import com.example.data.repository.KiranaRepository
import com.example.ui.screens.AiRemindersScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.KhataScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.theme.KiranaPrimary
import com.example.ui.theme.KiranaSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MANDATORY: Step 1 - Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                AppRootContent()
            }
        }
    }
}

@Composable
fun AppRootContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = currentUser!!.uid
        val email = currentUser!!.email ?: "दुकानदार"
        val displayName = currentUser!!.displayName ?: "किराना स्टोर"

        // Scoped key by uid to ensure state reset on user change
        key(uid) {
            KiranaStoreApp(
                currentUserId = uid,
                userEmail = email,
                userName = displayName
            )
        }
    }
}

private sealed class KiranaNavDestination(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    data object Khata : KiranaNavDestination("खाता", Icons.Default.AccountBalanceWallet, "nav_khata")
    data object AiReminder : KiranaNavDestination("AI तकादा", Icons.Default.AutoAwesome, "nav_ai_reminder")
    data object Inventory : KiranaNavDestination("इन्वेंटरी", Icons.Default.Inventory, "nav_inventory")
    data object Orders : KiranaNavDestination("ऑर्डर्स", Icons.Default.LocalShipping, "nav_orders")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiranaStoreApp(
    currentUserId: String,
    userEmail: String,
    userName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val kiranaRepository = remember(context) { KiranaRepository(context) }
    val aiReminderRepository = remember { AiReminderRepository() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var customerForAiReminder by remember { mutableStateOf<Customer?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val navDestinations = listOf(
        KiranaNavDestination.Khata,
        KiranaNavDestination.AiReminder,
        KiranaNavDestination.Inventory,
        KiranaNavDestination.Orders
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(KiranaPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = KiranaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "KiranaKhata",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = KiranaPrimary
                                )
                            )
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                navDestinations.forEachIndexed { index, dest ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            if (index != 1) {
                                customerForAiReminder = null
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = dest.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = dest.title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = KiranaPrimary,
                            selectedTextColor = KiranaPrimary,
                            indicatorColor = KiranaPrimary.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag(dest.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTab) {
                0 -> KhataScreen(
                    currentUserId = currentUserId,
                    repository = kiranaRepository,
                    onNavigateToAiReminder = { customer ->
                        customerForAiReminder = customer
                        selectedTab = 1
                    }
                )
                1 -> AiRemindersScreen(
                    currentUserId = currentUserId,
                    kiranaRepository = kiranaRepository,
                    aiReminderRepository = aiReminderRepository,
                    preselectedCustomer = customerForAiReminder
                )
                2 -> InventoryScreen(
                    currentUserId = currentUserId,
                    repository = kiranaRepository
                )
                3 -> OrdersScreen(
                    currentUserId = currentUserId,
                    repository = kiranaRepository
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("लॉगआउट करें?") },
            text = { Text("क्या आप सचमुच अपने Google खाते ($userEmail) से लॉगआउट करना चाहते हैं?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        val credentialManager = CredentialManager.create(context)
                        scope.launch {
                            withContext(NonCancellable) {
                                try {
                                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                } catch (e: Exception) {
                                    Log.w("Auth", "Failed to clear credential state: ${e.message}")
                                } finally {
                                    Firebase.auth.signOut()
                                }
                            }
                        }
                    }
                ) {
                    Text("लॉगआउट", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}
