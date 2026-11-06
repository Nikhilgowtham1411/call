package com.example.call.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.call.ui.components.FloralBackground
import com.example.call.ui.components.FloralHeader
import com.example.call.ui.components.PillButton
import com.example.call.ui.rememberCaller
import com.example.call.ui.theme.CallGreen
import com.example.call.ui.viewmodel.DialerViewModel

private data class Key(val main: String, val sub: String = "")

private val keyRows = listOf(
    listOf(Key("1"), Key("2", "ABC"), Key("3", "DEF")),
    listOf(Key("4", "GHI"), Key("5", "JKL"), Key("6", "MNO")),
    listOf(Key("7", "PQRS"), Key("8", "TUV"), Key("9", "WXYZ")),
    listOf(Key("*"), Key("0", "+"), Key("#"))
)

@Composable
fun DialerScreen(
    viewModel: DialerViewModel,
    onSaveNumber: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val number by viewModel.number.collectAsState()
    val call = rememberCaller()

    FloralBackground {
        Column(Modifier.fillMaxSize()) {
            FloralHeader(title = "Dialer")

            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = contentPadding.calculateBottomPadding() + 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))

                // Large number display with backspace.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(start = 24.dp, end = 8.dp)
                        .height(68.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = number.ifEmpty { "Enter number…" },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (number.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        fontSize = if (number.length > 12) 26.sp else 34.sp,
                        textAlign = TextAlign.Start
                    )
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .semantics { contentDescription = "Delete last digit" }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { viewModel.backspace() },
                                    onLongPress = { viewModel.clear() }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                if (number.isNotEmpty()) {
                    TextButton(onClick = { onSaveNumber(number) }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Save as new contact", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }

                Column(
                    Modifier
                        .weight(1f)
                        .widthIn(max = 380.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    keyRows.forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { key ->
                                DialKey(
                                    key = key,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) { viewModel.append(key.main.first()) }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                PillButton(
                    text = "Call",
                    onClick = { call(number) },
                    containerColor = CallGreen,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    height = 68.dp,
                    modifier = Modifier.widthIn(min = 220.dp)
                )
            }
        }
    }
}

@Composable
private fun DialKey(key: Key, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(76.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = key.main },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                key.main,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (key.sub.isNotEmpty()) {
                Text(
                    key.sub,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
