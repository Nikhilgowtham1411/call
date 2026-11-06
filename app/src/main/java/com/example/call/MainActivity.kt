package com.example.call

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.call.system.DialerRole
import com.example.call.system.Permissions
import com.example.call.ui.CallNavHost
import com.example.call.ui.components.LocalShowPhotos
import com.example.call.ui.theme.CallAppTheme
import com.example.call.ui.toast
import com.example.call.ui.viewmodel.AppViewModelProvider
import com.example.call.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels { AppViewModelProvider.Factory }

    /** Number from a tel: / DIAL intent, pre-filled into the dialer. */
    private var dialNumber by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        dialNumber = numberFrom(intent) ?: if (intent?.getBooleanExtra(EXTRA_OPEN_DIALER, false) == true) "" else null

        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            CallAppTheme(settings) {
                CompositionLocalProvider(LocalShowPhotos provides settings.showPhotos) {
                    AppRoot(
                        settingsViewModel = settingsViewModel,
                        dialNumber = dialNumber,
                        onDialNumberConsumed = { dialNumber = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val number = numberFrom(intent)
        when {
            number != null -> dialNumber = number
            intent.getBooleanExtra(EXTRA_OPEN_DIALER, false) -> dialNumber = ""
        }
    }

    companion object {
        /** Set by the in-call screen's "Add call" button: show the dialer. */
        const val EXTRA_OPEN_DIALER = "open_dialer"
    }

    private fun numberFrom(intent: Intent?): String? {
        val data: Uri = intent?.data ?: return null
        if (data.scheme != "tel") return null
        return data.schemeSpecificPart?.let { Uri.decode(it) }?.takeIf { it.isNotBlank() }
    }
}

@Composable
private fun AppRoot(
    settingsViewModel: SettingsViewModel,
    dialNumber: String?,
    onDialNumberConsumed: () -> Unit
) {
    val context = LocalContext.current

    // First launch: ask for everything the app needs in a single, friendly request.
    val askAll = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    var asked by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!asked && !Permissions.hasAll(context, Permissions.allRuntime())) {
            asked = true
            askAll.launch(Permissions.allRuntime())
        }
    }

    // Default phone app role request.
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (DialerRole.isDefaultDialer(context)) {
            toast(context, "Call is now your phone app")
        }
    }
    val setUpDialer: () -> Unit = {
        val intent = DialerRole.requestIntent(context)
        if (intent == null) {
            toast(context, "This phone does not allow changing the phone app")
        } else {
            runCatching { roleLauncher.launch(intent) }
                .onFailure { toast(context, "Could not open the phone app setting") }
        }
    }

    CallNavHost(
        setUpDialer = setUpDialer,
        dialNumber = dialNumber,
        onDialNumberConsumed = onDialNumberConsumed,
        settingsViewModel = settingsViewModel
    )
}
