package com.example.call.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.call.ui.theme.CallTheme
import com.example.call.ui.theme.DeepPurple
import com.example.call.ui.theme.Lavender

/** Set from the "Show contact photos" setting. When false every avatar shows initials. */
val LocalShowPhotos = compositionLocalOf { true }

/**
 * A contact picture. Uses the real contact photo URI; falls back to a pretty default avatar
 * when there is no photo, the photo cannot be loaded, or photos are turned off in settings.
 */
@Composable
fun ContactPhoto(
    photoUri: String?,
    name: String,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    initialSize: TextUnit = 28.sp
) {
    val showPhotos = LocalShowPhotos.current
    var failed by remember(photoUri) { mutableStateOf(false) }

    Box(modifier.clip(shape), contentAlignment = Alignment.Center) {
        DefaultAvatar(name = name, initialSize = initialSize)
        if (showPhotos && photoUri != null && !failed) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Photo of $name",
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun DefaultAvatar(name: String, initialSize: TextUnit = 28.sp, modifier: Modifier = Modifier) {
    val dark = CallTheme.colors.isDark
    val brush = Brush.linearGradient(
        if (dark) listOf(Color(0xFF5A4577), Color(0xFF3B2A52)) else listOf(Lavender, Color(0xFFF0E3FF))
    )
    val letter = name.trim().firstOrNull { it.isLetter() }?.uppercase()
    Box(
        modifier
            .fillMaxSize()
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        if (letter != null) {
            Text(
                text = letter,
                fontSize = initialSize,
                fontWeight = FontWeight.Bold,
                color = if (dark) Color.White else DeepPurple
            )
        } else {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = if (dark) Color.White else DeepPurple,
                modifier = Modifier.size(initialSize.value.dp * 1.4f)
            )
        }
    }
}

@Composable
fun CircleContactPhoto(photoUri: String?, name: String, size: Dp = 64.dp) {
    ContactPhoto(
        photoUri = photoUri,
        name = name,
        modifier = Modifier.size(size),
        shape = CircleShape,
        initialSize = (size.value * 0.45f).sp
    )
}

@Composable
fun SquareContactPhoto(photoUri: String?, name: String, modifier: Modifier = Modifier) {
    ContactPhoto(
        photoUri = photoUri,
        name = name,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
        initialSize = 64.sp
    )
}

@Suppress("unused")
private val keepTheme = MaterialTheme
