package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanDiamond
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinBright
import com.example.ui.theme.GoldContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

@Composable
fun PlayerAvatar(
    photoBase64: String,
    displayName: String,
    size: Dp = 48.dp,
    borderColor: Color = GoldCoin.copy(alpha = 0.7f),
    modifier: Modifier = Modifier
) {
    val bitmap: ImageBitmap? = remember(photoBase64) {
        if (photoBase64.isBlank()) {
            null
        } else {
            runCatching {
                val bytes = Base64.decode(photoBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }

    val initial = remember(displayName) {
        displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "V"
    }

    Surface(
        shape = CircleShape,
        border = BorderStroke(1.5.dp, borderColor),
        color = GoldContainer,
        modifier = modifier.size(size)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "$displayName avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(GoldContainer, CyanContainer)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = (size.value * 0.42f).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = GoldCoinBright
                )
            }
        }
    }
}

suspend fun encodeImageUriToCompactBase64(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val rawBitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val maxDim = maxOf(info.size.width, info.size.height)
                    if (maxDim > 320) {
                        val scale = 320f / maxDim.toFloat()
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1)
                        )
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }

            val sourceBitmap = rawBitmap ?: return@runCatching null
            val side = min(sourceBitmap.width, sourceBitmap.height).coerceAtLeast(1)
            val xOffset = (sourceBitmap.width - side) / 2
            val yOffset = (sourceBitmap.height - side) / 2
            val square = Bitmap.createBitmap(sourceBitmap, xOffset, yOffset, side, side)
            val scaled = Bitmap.createScaledBitmap(square, 160, 160, true)

            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 72, output)
            val bytes = output.toByteArray()

            if (square !== sourceBitmap) square.recycle()
            if (scaled !== square) scaled.recycle()

            if (bytes.size > 110_000) {
                val tightOut = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 50, tightOut)
                Base64.encodeToString(tightOut.toByteArray(), Base64.NO_WRAP)
            } else {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        }.getOrNull()
    }
