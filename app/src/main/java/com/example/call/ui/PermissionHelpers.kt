package com.example.call.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.call.system.CallPlacer
import com.example.call.system.Permissions

fun toast(context: Context, message: String) =
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()

/**
 * Returns a counter that increases every time the app returns to the foreground. Reading it in
 * a composable makes permission / role status re-evaluate after the user visits phone settings.
 */
@Composable
fun rememberResumeTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return tick
}

/** Asks for [permissions]; [onResult] receives true if all were granted. */
@Composable
fun rememberPermissionRequester(
    permissions: Array<String>,
    onResult: (Boolean) -> Unit
): () -> Unit {
    val current by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> current(result.values.all { it }) }
    return { launcher.launch(permissions) }
}

/**
 * Returns a function that places a call. If the phone permission is missing it asks for it first
 * and then continues with the same number, so the user only has to tap once more at most.
 */
@Composable
fun rememberCaller(): (String) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val number = pending
        pending = null
        if (number != null) {
            if (Permissions.canCall(context)) {
                report(context, CallPlacer.call(context, number))
            } else {
                toast(context, "Please allow phone calls to make a call")
            }
        }
    }

    return { number ->
        if (Permissions.canCall(context)) {
            report(context, CallPlacer.call(context, number))
        } else {
            pending = number
            launcher.launch(Permissions.phone)
        }
    }
}

private fun report(context: Context, outcome: CallPlacer.Outcome) {
    when (outcome) {
        is CallPlacer.Outcome.Failed -> toast(context, outcome.reason)
        CallPlacer.Outcome.NeedsPermission -> toast(context, "Please allow phone calls to make a call")
        CallPlacer.Outcome.Started -> Unit
    }
}
