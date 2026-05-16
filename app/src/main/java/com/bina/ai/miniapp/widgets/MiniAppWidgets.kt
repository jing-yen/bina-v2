package com.bina.ai.miniapp.widgets

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.bina.ai.miniapp.model.DataSet
import com.bina.ai.miniapp.model.Widget
import com.bina.ai.miniapp.runtime.VariableStore
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.OutfitFamily

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
        "heading" -> 24.sp to FontWeight.Bold
        "subheading" -> 18.sp to FontWeight.SemiBold
        "caption" -> 12.sp to FontWeight.Normal
        else -> 15.sp to FontWeight.Normal
    }
    val color = if (widget.color.isNotEmpty()) parseColor(widget.color) else {
        when (widget.style) {
            "caption" -> BinaGrayText
            "heading", "subheading" -> Color(0xFF1C1917)
            else -> Color(0xFF44403C)
        }
    }

    val hasMarkdown = text.contains("**") || text.contains("##") || text.contains("\n- ") || text.contains("\n* ") || text.startsWith("- ") || text.startsWith("* ")
    if (hasMarkdown && widget.style != "heading" && widget.style != "subheading") {
        SimpleMarkdown(text = text, baseColor = color, baseFontSize = size, fontWeight = weight, textAlign = align)
    } else {
        Text(
            text = text,
            fontSize = size,
            fontWeight = weight,
            color = color,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SimpleMarkdown(
    text: String,
    baseColor: Color,
    baseFontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    textAlign: TextAlign
) {
    val lines = text.split("\n")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        lines.forEach { line ->
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("### ") -> Text(
                    parseInlineMarkdown(trimmed.removePrefix("### ")),
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C1917)
                )
                trimmed.startsWith("## ") -> Text(
                    parseInlineMarkdown(trimmed.removePrefix("## ")),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1917)
                )
                trimmed.startsWith("# ") -> Text(
                    parseInlineMarkdown(trimmed.removePrefix("# ")),
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1917)
                )
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> Row(Modifier.fillMaxWidth()) {
                    Text("• ", fontSize = baseFontSize, color = baseColor)
                    Text(parseInlineMarkdown(trimmed.drop(2)), fontSize = baseFontSize, color = baseColor, modifier = Modifier.weight(1f))
                }
                trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val numEnd = trimmed.indexOf(". ")
                    Row(Modifier.fillMaxWidth()) {
                        Text("${trimmed.substring(0, numEnd + 1)} ", fontSize = baseFontSize, fontWeight = FontWeight.Medium, color = baseColor)
                        Text(parseInlineMarkdown(trimmed.substring(numEnd + 2)), fontSize = baseFontSize, color = baseColor, modifier = Modifier.weight(1f))
                    }
                }
                trimmed.isEmpty() -> Spacer(Modifier.height(4.dp))
                else -> Text(
                    parseInlineMarkdown(trimmed),
                    fontSize = baseFontSize, fontWeight = fontWeight, color = baseColor,
                    textAlign = textAlign, modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun parseInlineMarkdown(text: String) = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                    i = end + 2
                } else { append(text[i]); i++ }
            }
            text.startsWith("*", i) && !text.startsWith("**", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end > i) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end > i) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, background = Color(0x1A000000))) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
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
            color = Color(0xFF1C1917),
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }

    when (widget.inputType) {
        "dropdown" -> {
            var expanded by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, if (expanded) themeColor else Color(0xFFCACACA), RoundedCornerShape(14.dp))
                        .clickable(role = androidx.compose.ui.semantics.Role.Button) { expanded = true }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            value.ifEmpty { widget.hint.ifEmpty { "Select..." } },
                            fontSize = 14.sp,
                            color = if (value.isEmpty()) BinaGrayText else Color(0xFF1C1917)
                        )
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = BinaGrayText
                        )
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    widget.options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, fontSize = 14.sp) },
                            onClick = {
                                store[widget.bind] = option
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        "toggle" -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE5E5E5), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    widget.label.ifEmpty { widget.hint },
                    fontSize = 14.sp,
                    color = Color(0xFF1C1917)
                )
                Switch(
                    checked = value == "true",
                    onCheckedChange = { store[widget.bind] = it.toString() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = themeColor
                    )
                )
            }
        }
        else -> {
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
    }
}

// ── VoiceInput ─────────────────────────────────────────────

@Composable
fun VoiceInputWidget(
    widget: Widget.VoiceInput,
    store: VariableStore,
    themeColor: Color,
    inferenceEngine: com.bina.ai.inference.InferenceEngine? = null
) {
    val value = store[widget.bind]
    val context = LocalContext.current
    var recording by remember { mutableStateOf(false) }
    var transcribing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val recorderRef = remember { mutableStateOf<android.media.AudioRecord?>(null) }
    val audioFileRef = remember { mutableStateOf<java.io.File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            recorderRef.value?.release()
            recorderRef.value = null
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun startRecording() {
        val sampleRate = 16000
        val channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO
        val audioFormat = android.media.AudioFormat.ENCODING_PCM_FLOAT
        val bufferSize = android.media.AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            .coerceAtLeast(sampleRate * 4)

        val recorder = android.media.AudioRecord(
            android.media.MediaRecorder.AudioSource.MIC,
            sampleRate, channelConfig, audioFormat, bufferSize
        )
        if (recorder.state != android.media.AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            android.widget.Toast.makeText(context, "Could not initialize audio recorder", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val audioFile = java.io.File(context.cacheDir, "voice_${System.currentTimeMillis()}.wav")
        audioFileRef.value = audioFile
        recorderRef.value = recorder
        recording = true
        recorder.startRecording()

        scope.launch(Dispatchers.IO) {
            val buffer = FloatArray(1024)
            java.io.RandomAccessFile(audioFile, "rw").use { raf ->
                // Write WAV header placeholder (44 bytes)
                // Format: RIFF/WAVE, 16kHz, mono, 32-bit float (IEEE)
                val sr = 16000
                val channels = 1
                val bitsPerSample = 32
                val byteRate = sr * channels * bitsPerSample / 8
                val blockAlign = channels * bitsPerSample / 8
                val headerBuf = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                headerBuf.put("RIFF".toByteArray())
                headerBuf.putInt(0) // file size - 8 (filled later)
                headerBuf.put("WAVE".toByteArray())
                headerBuf.put("fmt ".toByteArray())
                headerBuf.putInt(16) // fmt chunk size
                headerBuf.putShort(3) // audio format: 3 = IEEE float
                headerBuf.putShort(channels.toShort())
                headerBuf.putInt(sr)
                headerBuf.putInt(byteRate)
                headerBuf.putShort(blockAlign.toShort())
                headerBuf.putShort(bitsPerSample.toShort())
                headerBuf.put("data".toByteArray())
                headerBuf.putInt(0) // data size (filled later)
                raf.write(headerBuf.array())

                var dataSize = 0L
                val sampleBuf = java.nio.ByteBuffer.allocate(buffer.size * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                while (recording) {
                    val read = recorder.read(buffer, 0, buffer.size, android.media.AudioRecord.READ_BLOCKING)
                    if (read > 0) {
                        sampleBuf.clear()
                        for (i in 0 until read) sampleBuf.putFloat(buffer[i])
                        raf.write(sampleBuf.array(), 0, read * 4)
                        dataSize += read * 4
                    }
                }

                // Patch WAV header with actual sizes
                raf.seek(4)
                val fileSizeBuf = java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                fileSizeBuf.putInt((36 + dataSize).toInt())
                raf.write(fileSizeBuf.array())
                raf.seek(40)
                val dataSizeBuf = java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                dataSizeBuf.putInt(dataSize.toInt())
                raf.write(dataSizeBuf.array())
            }
        }
    }

    fun stopAndTranscribe() {
        recording = false
        recorderRef.value?.stop()
        recorderRef.value?.release()
        recorderRef.value = null

        val audioFile = audioFileRef.value ?: return
        val engine = inferenceEngine
        if (engine == null || !engine.isReady) {
            android.widget.Toast.makeText(context, "AI model not ready", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        transcribing = true
        scope.launch {
            try {
                engine.generateWithAudio("Transcribe this audio.", audioFile.absolutePath)
                    .collect { result ->
                        store[widget.bind] = result.trim()
                    }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Transcription failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                transcribing = false
                audioFile.delete()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (recording) {
            Text("Recording...", fontSize = 13.sp, color = Color(0xFFDC2626), modifier = Modifier.weight(1f))
        } else if (transcribing) {
            Text("Transcribing...", fontSize = 13.sp, color = BinaGrayText, modifier = Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    when {
                        recording -> Color(0xFFDC2626)
                        transcribing -> themeColor.copy(alpha = 0.4f)
                        else -> themeColor
                    }
                )
                .clickable(enabled = !transcribing) {
                    if (recording) {
                        stopAndTranscribe()
                        return@clickable
                    }
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                transcribing -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                recording -> Icon(Icons.Filled.Stop, contentDescription = "Stop recording", tint = Color.White)
                else -> Icon(Icons.Filled.Mic, contentDescription = "Voice", tint = Color.White)
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
        val photoDir = java.io.File(context.filesDir, "photos").also { it.mkdirs() }
        photoDir.listFiles()?.filter { it.name.startsWith("camera_capture_") }?.forEach { it.delete() }
        val file = java.io.File(photoDir, "camera_capture_${System.currentTimeMillis()}.jpg")
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
            .height(if (hasPhoto) 200.dp else 100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (hasPhoto) Color.Transparent else Color(0xFF292524))
            .clickable(role = androidx.compose.ui.semantics.Role.Button) {
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
                    .height(if (hasPhoto) 200.dp else 100.dp)
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
                        contentDescription = "Photo captured",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Tap to retake", fontSize = 11.sp, color = Color.White)
                }
            }
        } else {
            Icon(
                Icons.Filled.CameraAlt,
                contentDescription = "Take a photo",
                modifier = Modifier.size(28.dp),
                tint = Color.White.copy(alpha = 0.7f)
            )
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = widget.columns
    ) {
        widget.buttons.forEach { button ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(themeColor)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button) { onAction(button.action) }
                    .padding(vertical = 14.dp, horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (button.icon.isNotEmpty()) {
                        Text(
                            button.icon,
                            fontSize = 22.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        button.label,
                        fontSize = 13.sp,
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
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
    var showConfirm by remember { mutableStateOf(false) }

    val handleClick: () -> Unit = {
        if (!isLoading) {
            if (widget.confirm.isNotEmpty()) showConfirm = true
            else onAction(widget.action)
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirm", fontWeight = FontWeight.SemiBold) },
            text = { Text(store.interpolate(widget.confirm), fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = { showConfirm = false; onAction(widget.action) },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) { Text("Confirm", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    when (widget.style) {
        "secondary" -> OutlinedButton(
            onClick = handleClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = themeColor
            ),
            border = BorderStroke(1.dp, themeColor),
            enabled = !isLoading
        ) {
            Text(label, fontSize = 15.sp, color = themeColor)
        }
        "danger" -> Button(
            onClick = handleClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            enabled = !isLoading
        ) {
            Text(label, fontSize = 15.sp, color = Color.White)
        }
        else -> Button(
            onClick = handleClick,
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            if (widget.emptyText.isNotEmpty()) {
                Text(widget.emptyText, fontSize = 14.sp, color = BinaGrayText)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE7E0D8).copy(alpha = 0.5f)))
                    Box(Modifier.fillMaxWidth(0.8f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE7E0D8).copy(alpha = 0.5f)))
                    Box(Modifier.fillMaxWidth(0.6f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE7E0D8).copy(alpha = 0.5f)))
                }
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
            SimpleMarkdown(
                text = content,
                baseColor = Color(0xFF1C1917),
                baseFontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Start
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
            .background(Color.White.copy(alpha = 0.7f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                display,
                fontSize = 22.sp,
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
    val context = LocalContext.current
    val locationStr = store["user_location"]
    if (dataSet == null) return

    val hasLocation = locationStr.isNotBlank()
    val userLat: Double
    val userLng: Double
    if (hasLocation) {
        val parts = locationStr.split(",")
        userLat = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
        userLng = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
    } else {
        userLat = 0.0
        userLng = 0.0
    }

    val sorted = if (hasLocation) {
        dataSet.items
            .filter { it.lat != 0.0 || it.lng != 0.0 }
            .map { it to haversine(userLat, userLng, it.lat, it.lng) }
            .sortedBy { it.second }
            .take(widget.limit)
    } else {
        dataSet.items.filter { it.lat != 0.0 || it.lng != 0.0 }.take(widget.limit).map { it to -1.0 }
    }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (hasLocation) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeColor.copy(alpha = 0.08f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📍", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "You: ${String.format("%.4f", userLat)}, ${String.format("%.4f", userLng)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = themeColor
                )
            }
        }

        sorted.forEach { (point, distance) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable {
                        try {
                            val uri = android.net.Uri.parse(
                                "geo:${point.lat},${point.lng}?q=${point.lat},${point.lng}(${android.net.Uri.encode(point.name)})"
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val webUri = android.net.Uri.parse(
                                "https://maps.google.com/?q=${point.lat},${point.lng}"
                            )
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, webUri))
                        }
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = "Location",
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
                if (widget.showDistance && distance >= 0) {
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

// ── ProgressBar ───────────────────────────────────────────

@Composable
fun ProgressBarWidget(widget: Widget.ProgressBar, store: VariableStore, themeColor: Color) {
    val current = store[widget.bind].toIntOrNull() ?: 0
    val progress = (current.toFloat() / widget.total.coerceAtLeast(1)).coerceIn(0f, 1f)
    val isComplete = current >= widget.total

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (isComplete) "Complete!" else "Step $current of ${widget.total}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isComplete) themeColor else Color(0xFF1C1917)
            )
            Text(
                "${(progress * 100).toInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = themeColor
            )
        }
        Spacer(Modifier.height(6.dp))
        androidx.compose.material3.LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = themeColor,
            trackColor = themeColor.copy(alpha = 0.12f),
        )
    }
}

// ── ChecklistItems ────────────────────────────────────────

@Composable
fun ChecklistItemsWidget(widget: Widget.ChecklistItems, store: VariableStore, themeColor: Color) {
    val currentStep = store[widget.bind].toIntOrNull() ?: 0
    val isComplete = currentStep >= widget.items.size

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        widget.items.forEachIndexed { index, item ->
            val isDone = index < currentStep
            val isCurrent = index == currentStep && !isComplete
            val isFuture = index > currentStep

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isCurrent -> themeColor.copy(alpha = 0.1f)
                            else -> Color.White
                        }
                    )
                    .border(
                        width = if (isCurrent) 1.5.dp else 0.dp,
                        color = if (isCurrent) themeColor.copy(alpha = 0.3f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDone) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Step completed",
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(
                                2.dp,
                                if (isCurrent) themeColor else BinaGrayText.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) themeColor else BinaGrayText
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    item.label,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        isDone -> BinaGrayText
                        isFuture -> BinaGrayText.copy(alpha = 0.6f)
                        else -> Color(0xFF1C1917)
                    },
                    textDecoration = if (isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
            }
        }

        if (isComplete) {
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(themeColor.copy(alpha = 0.08f))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "All steps completed!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${widget.items.size} of ${widget.items.size} steps done",
                    fontSize = 13.sp,
                    color = BinaGrayText
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { store[widget.bind] = "0" },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, themeColor)
                ) {
                    Text("Reset Checklist", color = themeColor, fontWeight = FontWeight.Medium)
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
    Color(0xFFC45A3A)
}
