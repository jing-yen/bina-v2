package com.bina.ai.miniapp.widgets

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bina.ai.miniapp.model.DataSet
import com.bina.ai.miniapp.model.Widget
import com.bina.ai.miniapp.runtime.VariableStore
import com.bina.ai.ui.theme.BinaGrayText

// ── TextLabel ──────────────────────────────────────────────

@Composable
fun TextLabelWidget(widget: Widget.TextLabel, store: VariableStore, themeColor: Color) {
    val text = store.interpolate(widget.text)
    val align = when (widget.align) {
        "center" -> TextAlign.Center
        "right" -> TextAlign.End
        else -> TextAlign.Start
    }
    val (size, weight) = when (widget.style) {
        "heading" -> 22.sp to FontWeight.Bold
        "subheading" -> 17.sp to FontWeight.SemiBold
        "caption" -> 12.sp to FontWeight.Normal
        else -> 15.sp to FontWeight.Normal
    }
    val color = if (widget.color.isNotEmpty()) parseColor(widget.color) else {
        if (widget.style == "caption") BinaGrayText else Color(0xFF1A1A2E)
    }

    Text(
        text = text,
        fontSize = size,
        fontWeight = weight,
        color = color,
        textAlign = align,
        modifier = Modifier.fillMaxWidth()
    )
}

// ── TextInput ──────────────────────────────────────────────

@Composable
fun TextInputWidget(widget: Widget.TextInput, store: VariableStore, themeColor: Color) {
    val value = store[widget.bind]

    if (widget.label.isNotEmpty()) {
        Text(
            widget.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1A2E),
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }

    OutlinedTextField(
        value = value,
        onValueChange = { store[widget.bind] = it },
        placeholder = { Text(widget.hint, color = BinaGrayText, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = themeColor,
            cursorColor = themeColor
        ),
        keyboardOptions = when (widget.inputType) {
            "number" -> KeyboardOptions(keyboardType = KeyboardType.Number)
            else -> KeyboardOptions.Default
        },
        singleLine = widget.inputType != "multiline",
        minLines = if (widget.inputType == "multiline") 3 else 1
    )
}

// ── VoiceInput ─────────────────────────────────────────────

@Composable
fun VoiceInputWidget(widget: Widget.VoiceInput, store: VariableStore, themeColor: Color) {
    val value = store[widget.bind]
    val context = LocalContext.current
    var listening by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        listening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: ""
            if (spoken.isNotEmpty()) {
                store[widget.bind] = spoken
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, widget.hint.ifEmpty { "Speak now..." })
            }
            listening = true
            speechLauncher.launch(intent)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { store[widget.bind] = it },
            placeholder = { Text(widget.hint, color = BinaGrayText, fontSize = 14.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColor,
                cursorColor = themeColor
            ),
            singleLine = true
        )

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (listening) themeColor.copy(alpha = 0.6f) else themeColor)
                .clickable {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, widget.hint.ifEmpty { "Speak now..." })
                        }
                        listening = true
                        speechLauncher.launch(intent)
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (listening) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Filled.Mic, contentDescription = "Voice", tint = Color.White)
            }
        }
    }
}

// ── CameraInput ────────────────────────────────────────────

@Composable
fun CameraInputWidget(widget: Widget.CameraInput, store: VariableStore, themeColor: Color) {
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val hasPhoto = capturedBitmap != null

    var photoFile by remember { mutableStateOf<java.io.File?>(null) }
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    fun createPhotoFile(): Pair<java.io.File, android.net.Uri> {
        val file = java.io.File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        return file to uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile != null) {
            val bmp = android.graphics.BitmapFactory.decodeFile(photoFile!!.absolutePath)
            capturedBitmap = bmp
            store[widget.bind] = photoFile!!.absolutePath
            android.util.Log.d("CameraInput", "Photo saved: ${photoFile!!.absolutePath} (${photoFile!!.length()} bytes)")
        }
    }

    fun launchCamera() {
        val (file, uri) = createPhotoFile()
        photoFile = file
        photoUri = uri
        cameraLauncher.launch(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (hasPhoto) 200.dp else 120.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .background(themeColor.copy(alpha = 0.05f))
            .clickable {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    launchCamera()
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (hasPhoto && capturedBitmap != null) {
            Image(
                bitmap = capturedBitmap!!.asImageBitmap(),
                contentDescription = "Captured photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (hasPhoto) 200.dp else 120.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Tap to retake", fontSize = 11.sp, color = Color.White)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = "Camera",
                    modifier = Modifier.size(32.dp),
                    tint = themeColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    widget.label,
                    fontSize = 14.sp,
                    color = BinaGrayText
                )
            }
        }
    }
}

// ── MacroGrid ──────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MacroGridWidget(
    widget: Widget.MacroGrid,
    themeColor: Color,
    onAction: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = widget.columns
    ) {
        widget.buttons.forEach { button ->
            val btnColor = if (button.color.isNotEmpty()) parseColor(button.color) else themeColor

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(btnColor.copy(alpha = 0.1f))
                    .border(1.dp, btnColor.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .clickable { onAction(button.action) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    button.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = btnColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

// ── Slider ─────────────────────────────────────────────────

@Composable
fun SliderWidget(widget: Widget.Slider, store: VariableStore, themeColor: Color) {
    val current = store.getNumber(widget.bind).toFloat().coerceIn(widget.min, widget.max)

    Column(Modifier.fillMaxWidth()) {
        if (widget.label.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(widget.label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (widget.showValue) {
                    Text(
                        current.toInt().toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColor
                    )
                }
            }
        }

        Slider(
            value = current,
            onValueChange = { store[widget.bind] = it.toInt().toString() },
            valueRange = widget.min..widget.max,
            steps = ((widget.max - widget.min) / widget.step).toInt() - 1,
            colors = SliderDefaults.colors(
                thumbColor = themeColor,
                activeTrackColor = themeColor
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (widget.leftLabel.isNotEmpty() || widget.rightLabel.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(widget.leftLabel, fontSize = 11.sp, color = BinaGrayText)
                Text(widget.rightLabel, fontSize = 11.sp, color = BinaGrayText)
            }
        }
    }
}

// ── ActionButton ───────────────────────────────────────────

@Composable
fun ActionButtonWidget(
    widget: Widget.ActionButton,
    store: VariableStore,
    themeColor: Color,
    isLoading: Boolean,
    onAction: (String) -> Unit
) {
    val label = if (widget.icon.isNotEmpty()) "${widget.icon} ${widget.label}" else widget.label

    when (widget.style) {
        "secondary" -> OutlinedButton(
            onClick = { if (!isLoading) onAction(widget.action) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = !isLoading
        ) {
            Text(label, fontSize = 15.sp)
        }
        "danger" -> Button(
            onClick = { if (!isLoading) onAction(widget.action) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            enabled = !isLoading
        ) {
            Text(label, fontSize = 15.sp, color = Color.White)
        }
        else -> Button(
            onClick = { if (!isLoading) onAction(widget.action) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(label, fontSize = 15.sp, color = Color.White)
        }
    }
}

// ── MarkdownOutput ─────────────────────────────────────────

@Composable
fun MarkdownOutputWidget(widget: Widget.MarkdownOutput, store: VariableStore, themeColor: Color) {
    val content = store[widget.source]
    val isLoading = store.isTrue("is_loading")

    if (content.isEmpty() && !isLoading) {
        if (widget.emptyText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF0F4FF))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(widget.emptyText, fontSize = 14.sp, color = BinaGrayText)
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column {
            Text(
                content,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = Color(0xFF1A1A2E)
            )
            if (isLoading && widget.streaming) {
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = themeColor,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

// ── MetricCard ─────────────────────────────────────────────

@Composable
fun MetricCardWidget(widget: Widget.MetricCard, store: VariableStore, themeColor: Color) {
    val raw = store.getNumber(widget.source)
    val formatted = when (widget.format) {
        "integer" -> raw.toLong().toString()
        "decimal_1" -> String.format("%.1f", raw)
        else -> String.format("%.2f", raw)
    }
    val display = "${widget.prefix}$formatted${widget.suffix}"
    val numColor = if (widget.color.isNotEmpty()) parseColor(widget.color) else themeColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(numColor.copy(alpha = 0.08f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                display,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = numColor
            )
            if (widget.label.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(widget.label, fontSize = 13.sp, color = BinaGrayText)
            }
        }
    }
}

// ── GeoDisplay ─────────────────────────────────────────────

@Composable
fun GeoDisplayWidget(
    widget: Widget.GeoDisplay,
    store: VariableStore,
    dataSet: DataSet?,
    themeColor: Color
) {
    val locationStr = store["user_location"]
    if (locationStr.isBlank() || dataSet == null) {
        if (widget.emptyText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF0F4FF))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(widget.emptyText, fontSize = 14.sp, color = BinaGrayText)
            }
        }
        return
    }

    val parts = locationStr.split(",")
    val userLat = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
    val userLng = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0

    val sorted = dataSet.items
        .map { it to haversine(userLat, userLng, it.lat, it.lng) }
        .sortedBy { it.second }
        .take(widget.limit)

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sorted.forEach { (point, distance) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(point.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (point.info.isNotEmpty()) {
                        Text(point.info, fontSize = 12.sp, color = BinaGrayText)
                    }
                }
                if (widget.showDistance) {
                    Text(
                        String.format("%.1f km", distance),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColor
                    )
                }
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────

fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2)
    return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color(0xFF091A7A)
}
