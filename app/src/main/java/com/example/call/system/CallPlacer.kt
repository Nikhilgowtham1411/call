package com.example.call.system

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager

/** Starts outgoing calls through the Telecom framework. */
object CallPlacer {

    sealed interface Outcome {
        data object Started : Outcome
        data object NeedsPermission : Outcome
        data class Failed(val reason: String) : Outcome
    }

    fun call(context: Context, number: String): Outcome {
        val clean = number.trim()
        if (clean.isEmpty()) return Outcome.Failed("Please enter a phone number")
        if (!Permissions.canCall(context)) return Outcome.NeedsPermission
        val telecom = context.getSystemService(TelecomManager::class.java)
            ?: return Outcome.Failed("This device cannot make calls")
        return try {
            telecom.placeCall(Uri.fromParts("tel", clean, null), Bundle())
            Outcome.Started
        } catch (e: SecurityException) {
            Outcome.NeedsPermission
        } catch (e: Exception) {
            Outcome.Failed(e.message ?: "The call could not be started")
        }
    }
}
