package com.bina.ai.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.inference.DownloadState
import com.bina.ai.inference.ModelDownloadManager
import kotlinx.coroutines.launch

@Composable
fun ModelDownloadScreen(
    downloadManager: ModelDownloadManager,
    onModelReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by downloadManager.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state) {
        if (state is DownloadState.Done) {
            onModelReady()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🧠",
            fontSize = 56.sp
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "AI Model Required",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1917)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Bina needs a ~2.6 GB language model to work offline. Download once, use forever.",
            fontSize = 14.sp,
            color = Color(0xFF78716C),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(32.dp))

        when (val s = state) {
            is DownloadState.Idle -> {
                Button(
                    onClick = { scope.launch { downloadManager.download() } },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5B6ABF)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Download Gemma 4 E2B", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "~2.6 GB · WiFi recommended",
                    fontSize = 12.sp,
                    color = Color(0xFFA8A29E)
                )
            }

            is DownloadState.Downloading -> {
                val animatedProgress by animateFloatAsState(
                    targetValue = if (s.progress >= 0f) s.progress else 0f,
                    animationSpec = tween(300),
                    label = "progress"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE7E5E4))
                ) {
                    if (s.progress >= 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF5B6ABF))
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF5B6ABF),
                            trackColor = Color.Transparent
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatBytes(s.downloadedBytes),
                        fontSize = 13.sp,
                        color = Color(0xFF78716C)
                    )
                    if (s.totalBytes > 0) {
                        Text(
                            text = "${(s.progress * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF5B6ABF)
                        )
                    }
                    Text(
                        text = formatBytes(s.totalBytes),
                        fontSize = 13.sp,
                        color = Color(0xFF78716C)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Downloading… keep the app open",
                    fontSize = 12.sp,
                    color = Color(0xFFA8A29E)
                )
            }

            is DownloadState.Done -> {
                Text(
                    text = "✅ Model ready",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF16A34A)
                )
            }

            is DownloadState.Error -> {
                Text(
                    text = s.message,
                    fontSize = 13.sp,
                    color = Color(0xFFDC2626),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { scope.launch { downloadManager.download() } },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5B6ABF)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Retry Download", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        "%.1f GB".format(mb / 1024)
    } else {
        "%.0f MB".format(mb)
    }
}
