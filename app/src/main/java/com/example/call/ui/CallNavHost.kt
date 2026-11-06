package com.example.call.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import com.example.call.ui.screens.ContactEditScreen
import com.example.call.ui.screens.ContactsScreen
import com.example.call.ui.screens.DialerScreen
import com.example.call.ui.screens.HomeScreen
import com.example.call.ui.screens.LogsScreen
import com.example.call.ui.screens.SettingsScreen
import com.example.call.ui.viewmodel.AppViewModelProvider
import com.example.call.ui.viewmodel.CallLogViewModel
import com.example.call.ui.viewmodel.ContactsViewModel
import com.example.call.ui.viewmodel.DialerViewModel
import com.example.call.ui.viewmodel.SettingsViewModel

private object Route {
    const val HOME = "home"
    const val DIALER = "dialer"
    const val LOGS = "logs"
    const val CONTACTS = "contacts"
    const val SETTINGS = "settings"
    const val EDIT = "edit?contactId={contactId}&number={number}"
    fun edit(contactId: Long = -1L, number: String = "") =
        "edit?contactId=$contactId&number=${android.net.Uri.encode(number)}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Route.HOME, "Home", Icons.Filled.Home),
    Tab(Route.DIALER, "Dialer", Icons.Filled.Dialpad),
    Tab(Route.LOGS, "Logs", Icons.Filled.History),
    Tab(Route.CONTACTS, "Contacts", Icons.Filled.Contacts)
)

@Composable
fun CallNavHost(
    setUpDialer: () -> Unit,
    dialNumber: String?,
    onDialNumberConsumed: () -> Unit,
    settingsViewModel: SettingsViewModel
) {
    val nav = rememberNavController()
    val contactsVm: ContactsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val logVm: CallLogViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val dialerVm: DialerViewModel = viewModel(factory = AppViewModelProvider.Factory)

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        contactsVm.messages.collect { snackbar.showSnackbar(it) }
    }

    // A tel: link opened the app: show it in the dialer.
    LaunchedEffect(dialNumber) {
        if (dialNumber != null) {
            dialerVm.set(dialNumber)
            nav.navigateTab(Route.DIALER)
            onDialNumberConsumed()
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = currentRoute in tabs.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.navigateTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    ) { inner ->
        // Each screen draws its own header edge to edge, so only the bottom inset is passed on.
        val bottomOnly = PaddingValues(bottom = inner.calculateBottomPadding())
        Box(Modifier.fillMaxSize().padding(bottom = 0.dp)) {
            NavHost(
                navController = nav,
                startDestination = Route.HOME,
                enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 } },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { it / 12 } }
            ) {
                composable(Route.HOME) {
                    HomeScreen(
                        viewModel = contactsVm,
                        onOpenSettings = { nav.navigate(Route.SETTINGS) },
                        onSetUpDialer = setUpDialer,
                        contentPadding = bottomOnly
                    )
                }
                composable(Route.DIALER) {
                    DialerScreen(
                        viewModel = dialerVm,
                        onSaveNumber = { nav.navigate(Route.edit(number = it)) },
                        contentPadding = bottomOnly
                    )
                }
                composable(Route.LOGS) {
                    LogsScreen(viewModel = logVm, contentPadding = bottomOnly)
                }
                composable(Route.CONTACTS) {
                    ContactsScreen(
                        viewModel = contactsVm,
                        onAdd = { nav.navigate(Route.edit()) },
                        onEdit = { nav.navigate(Route.edit(contactId = it.id)) },
                        contentPadding = bottomOnly
                    )
                }
                composable(Route.SETTINGS) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { nav.popBackStack() },
                        onSetUpDialer = setUpDialer,
                        contentPadding = bottomOnly
                    )
                }
                composable(
                    route = Route.EDIT,
                    arguments = listOf(
                        navArgument("contactId") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("number") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { entry ->
                    val id = entry.arguments?.getLong("contactId") ?: -1L
                    val number = entry.arguments?.getString("number").orEmpty()
                    ContactEditScreen(
                        viewModel = contactsVm,
                        existing = if (id >= 0) contactsVm.contactById(id) else null,
                        initialNumber = number,
                        onClose = {
                            dialerVm.clear()
                            nav.popBackStack()
                        },
                        contentPadding = bottomOnly
                    )
                }
            }
        }
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
