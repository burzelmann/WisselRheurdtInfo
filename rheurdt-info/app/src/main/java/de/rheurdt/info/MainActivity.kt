package de.rheurdt.info

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rheurdt.info.ui.RheurdtViewModel
import de.rheurdt.info.ui.screens.CalendarScreen
import de.rheurdt.info.ui.screens.DashboardScreen
import de.rheurdt.info.ui.screens.MapScreen
import de.rheurdt.info.ui.screens.PoliticsScreen
import de.rheurdt.info.ui.screens.SettingsScreen
import de.rheurdt.info.ui.theme.RheurdtTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RheurdtViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RheurdtTheme {
                val initialTab = if (intent?.getStringExtra("target_tab") == "politics") 2 else 0
                var selectedTab by remember { mutableStateOf(initialTab) }
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val context = LocalContext.current

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    // Notification permission handled
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Start") },
                                label = { Text("Start") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Kalender") },
                                label = { Text("Kalender") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Politik") },
                                label = { Text("Politik") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.Map, contentDescription = "Karte") },
                                label = { Text("Karte") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 4,
                                onClick = { selectedTab = 4 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Setup") },
                                label = { Text("Setup") }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        0 -> DashboardScreen(
                            state = uiState,
                            onRefresh = { viewModel.refresh() },
                            onSelectBusStop = { viewModel.selectBusStop(it) },
                            onRefreshBus = { viewModel.refreshBus() },
                            onNavigateToCalendar = { selectedTab = 1 },
                            onNavigateToPolitics = { selectedTab = 2 },
                            onMoveCard = { from, to -> viewModel.moveCard(from, to) },
                            onDisableCard = { viewModel.disableCard(it) },
                            onEnableCard = { viewModel.enableCard(it) },
                            modifier = Modifier.padding(innerPadding)
                        )
                        1 -> CalendarScreen(
                            state = uiState,
                            modifier = Modifier.padding(innerPadding)
                        )
                        2 -> PoliticsScreen(
                            state = uiState,
                            modifier = Modifier.padding(innerPadding)
                        )
                        3 -> MapScreen(
                            state = uiState,
                            modifier = Modifier.padding(innerPadding)
                        )
                        4 -> SettingsScreen(
                            state = uiState,
                            onStreetChanged = { viewModel.updateStreet(it) },
                            onPvCapacityChanged = { viewModel.updatePvCapacity(it) },
                            onToggleNotification = { viewModel.toggleWasteNotification(it) },
                            onSendTestNotification = { viewModel.sendTestNotification(it) },
                            onTogglePoliticsMeetingNotification = { viewModel.togglePoliticsMeetingNotification(it) },
                            onTogglePoliticsDecisionNotification = { viewModel.togglePoliticsDecisionNotification(it) },
                            onSendTestPoliticsMeeting = { viewModel.sendTestPoliticsMeetingNotification(it) },
                            onSendTestPoliticsDecision = { viewModel.sendTestPoliticsDecisionNotification(it) },
                            onToggleSevereAlertsNotification = { viewModel.toggleSevereAlertsNotification(it) },
                            onTogglePegelAlertsNotification = { viewModel.togglePegelAlertsNotification(it) },
                            onSendTestSevereAlert = { viewModel.sendTestSevereAlertNotification(it) },
                            onSendTestPegelAlert = { viewModel.sendTestPegelAlertNotification(it) },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
