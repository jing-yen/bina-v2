package com.bina.ai.ui.screens.studio

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.theme.BinaAccent
import com.bina.ai.ui.theme.BinaBgCard
import com.bina.ai.ui.theme.BinaGrayBorder
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaStone950
import com.bina.ai.ui.theme.BinaTurmeric
import java.io.File

data class WidgetToggle(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val description: String,
    var enabled: Boolean = false
)

data class UploadedFile(
    val name: String,
    val size: String,
    val uri: Uri
)

@Composable
fun StudioScreen(onPublished: () -> Unit = {}) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }

    // Step 1 state
    var recipeName by remember { mutableStateOf("") }
    var recipeDescription by remember { mutableStateOf("") }
    var recipeIcon by remember { mutableStateOf("🤖") }
    var systemPrompt by remember { mutableStateOf("") }
    var blockedKeywords by remember { mutableStateOf("") }
    var disclaimer by remember { mutableStateOf("") }

    // Step 2 state
    val uploadedFiles = remember { mutableStateListOf<UploadedFile>() }
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            var name = "document"
            var size = 0L
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIdx >= 0) name = it.getString(nameIdx)
                    if (sizeIdx >= 0) size = it.getLong(sizeIdx)
                }
            }
            val sizeStr = when {
                size > 1_000_000 -> "%.1f MB".format(size / 1_000_000.0)
                size > 1_000 -> "%.0f KB".format(size / 1_000.0)
                else -> "$size B"
            }
            uploadedFiles.add(UploadedFile(name, sizeStr, uri))
        }
    }

    // Step 3 state
    val widgets = remember {
        mutableStateListOf(
            WidgetToggle("camera", "Camera Input", Icons.Default.CameraAlt, "Photo capture & vision AI"),
            WidgetToggle("voice", "Voice Input", Icons.Default.Mic, "Speech-to-text input"),
            WidgetToggle("location", "Location", Icons.Default.LocationOn, "GPS & nearby places"),
            WidgetToggle("calculator", "Calculator", Icons.Default.Calculate, "Formula-based computations"),
            WidgetToggle("text", "Text Input", Icons.Default.TextFields, "Freeform text entry", enabled = true)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text("Recipe Studio", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = BinaStone950)
            Text("Create AI recipes for grassroots users", fontSize = 14.sp, color = BinaGrayText)
        }

        // Progress Steps
        StepIndicator(currentStep)
        Spacer(Modifier.height(16.dp))

        // Step Content
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn())
                    .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut())
            },
            label = "step"
        ) { step ->
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                when (step) {
                    1 -> StepIdentity(
                        recipeName = recipeName, onNameChange = { recipeName = it },
                        recipeDescription = recipeDescription, onDescriptionChange = { recipeDescription = it },
                        recipeIcon = recipeIcon, onIconChange = { recipeIcon = it },
                        systemPrompt = systemPrompt, onPromptChange = { systemPrompt = it },
                        blockedKeywords = blockedKeywords, onBlockedChange = { blockedKeywords = it },
                        disclaimer = disclaimer, onDisclaimerChange = { disclaimer = it }
                    )
                    2 -> StepKnowledge(
                        uploadedFiles = uploadedFiles,
                        onPickFile = { filePicker.launch(arrayOf("application/pdf", "text/plain", "text/csv")) },
                        onRemoveFile = { uploadedFiles.removeAt(it) }
                    )
                    3 -> StepWidgets(
                        widgets = widgets,
                        onToggle = { idx ->
                            val w = widgets[idx]
                            widgets[idx] = w.copy(enabled = !w.enabled)
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Bottom Actions
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            // Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Previous", fontWeight = FontWeight.SemiBold)
                    }
                }
                if (currentStep < 3) {
                    Button(
                        onClick = { currentStep++ },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BinaAccent)
                    ) {
                        Text("Next Step", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = {
                            val yaml = generateYaml(
                                recipeName, recipeDescription, recipeIcon,
                                systemPrompt, blockedKeywords, disclaimer,
                                uploadedFiles, widgets
                            )
                            val id = recipeName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                                .ifEmpty { "my_recipe" }
                            val file = File(context.filesDir, "miniapps/$id.yaml")
                            file.parentFile?.mkdirs()
                            file.writeText(yaml)

                            // Copy knowledge files
                            val knowledgeDir = File(context.filesDir, "knowledge/$id")
                            knowledgeDir.mkdirs()
                            uploadedFiles.forEach { uf ->
                                try {
                                    val input = context.contentResolver.openInputStream(uf.uri)
                                    val dest = File(knowledgeDir, uf.name)
                                    input?.use { it.copyTo(dest.outputStream()) }
                                } catch (_: Exception) {}
                            }

                            Toast.makeText(context, "Recipe published! ✓", Toast.LENGTH_SHORT).show()
                            onPublished()
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BinaGreen
                        )
                    ) {
                        Text("Publish to Hub", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StepIndicator(currentStep: Int) {
    val steps = listOf("Identity", "Knowledge", "Widgets")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val stepNum = index + 1
            val isActive = currentStep == stepNum
            val isCompleted = currentStep > stepNum

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive || isCompleted) BinaAccent else BinaBgCard
                        )
                        .border(2.dp, if (isActive || isCompleted) BinaAccent else BinaGrayBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            "$stepNum",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isActive) Color.White else BinaGrayText
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isActive) BinaAccent else BinaGrayText
                )
            }

            if (index < steps.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 8.dp)
                        .background(if (currentStep > stepNum) BinaAccent else BinaGrayBorder)
                )
            }
        }
    }
}

@Composable
private fun StepIdentity(
    recipeName: String, onNameChange: (String) -> Unit,
    recipeDescription: String, onDescriptionChange: (String) -> Unit,
    recipeIcon: String, onIconChange: (String) -> Unit,
    systemPrompt: String, onPromptChange: (String) -> Unit,
    blockedKeywords: String, onBlockedChange: (String) -> Unit,
    disclaimer: String, onDisclaimerChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Define Your Recipe", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = BinaStone950)
        Text(
            "Set the AI's identity, personality, and safety boundaries",
            fontSize = 12.sp, color = BinaGrayText
        )

        Spacer(Modifier.height(4.dp))

        // Name & Icon row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = recipeIcon,
                onValueChange = { if (it.length <= 2) onIconChange(it) },
                label = { Text("Icon") },
                modifier = Modifier.width(80.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = studioFieldColors()
            )
            OutlinedTextField(
                value = recipeName,
                onValueChange = onNameChange,
                label = { Text("Recipe Name") },
                placeholder = { Text("e.g. Health Buddy") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = studioFieldColors()
            )
        }

        OutlinedTextField(
            value = recipeDescription,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            placeholder = { Text("What does this recipe do?") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = studioFieldColors()
        )

        // System prompt
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(BinaBgCard.copy(alpha = 0.92f))
                .padding(16.dp)
        ) {
            Column {
                Text("System Prompt", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BinaStone950)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = onPromptChange,
                    placeholder = { Text("You are a helpful medical assistant for rural midwives...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 6,
                    maxLines = 10,
                    colors = studioFieldColors()
                )
            }
        }

        // Safety
        OutlinedTextField(
            value = blockedKeywords,
            onValueChange = onBlockedChange,
            label = { Text("Blocked Keywords") },
            placeholder = { Text("dangerous, poison, illegal (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = studioFieldColors()
        )

        OutlinedTextField(
            value = disclaimer,
            onValueChange = onDisclaimerChange,
            label = { Text("Safety Disclaimer") },
            placeholder = { Text("AI-generated content. Not professional advice.") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = studioFieldColors()
        )

        // Tip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BinaTurmeric.copy(alpha = 0.1f))
                .padding(12.dp)
        ) {
            Text(
                "💡 Tip: Be specific about the user's context, language level, and safety constraints. This prompt runs entirely on-device.",
                fontSize = 12.sp, color = BinaStone950, lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun StepKnowledge(
    uploadedFiles: List<UploadedFile>,
    onPickFile: () -> Unit,
    onRemoveFile: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Upload Knowledge Base", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = BinaStone950)
        Text(
            "Add PDFs, text files, or CSVs to enhance the AI's knowledge",
            fontSize = 12.sp, color = BinaGrayText
        )

        Spacer(Modifier.height(4.dp))

        // Upload area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, BinaGrayBorder, RoundedCornerShape(20.dp))
                .background(BinaBgCard.copy(alpha = 0.6f))
                .clickable { onPickFile() }
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BinaAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Upload, null, tint = BinaAccent, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Tap to browse files", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BinaStone950)
                Text("PDF, TXT, CSV up to 10MB", fontSize = 12.sp, color = BinaGrayText)
            }
        }

        // Uploaded files
        uploadedFiles.forEachIndexed { index, file ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BinaBgCard.copy(alpha = 0.92f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BinaGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Description, null, tint = BinaGreen, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(file.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BinaStone950, maxLines = 1)
                    Text(file.size, fontSize = 12.sp, color = BinaGrayText)
                }
                Icon(Icons.Default.Check, null, tint = BinaGreen, modifier = Modifier.size(20.dp))
            }
        }

        // Info box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BinaTurmeric.copy(alpha = 0.1f))
                .padding(12.dp)
        ) {
            Text(
                "📚 Files are embedded locally using LiteRT. The AI can reference this knowledge even offline.",
                fontSize = 12.sp, color = BinaStone950, lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun StepWidgets(
    widgets: List<WidgetToggle>,
    onToggle: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Widget Permissions", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = BinaStone950)
        Text(
            "Choose which capabilities the AI recipe can use",
            fontSize = 12.sp, color = BinaGrayText
        )

        Spacer(Modifier.height(4.dp))

        widgets.forEachIndexed { index, widget ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (widget.enabled) BinaGreen.copy(alpha = 0.08f)
                        else BinaBgCard.copy(alpha = 0.92f)
                    )
                    .border(
                        1.dp,
                        if (widget.enabled) BinaGreen.copy(alpha = 0.25f) else Color.Transparent,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onToggle(index) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (widget.enabled) BinaGreen else BinaGrayBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(widget.icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(widget.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BinaStone950)
                    Text(widget.description, fontSize = 12.sp, color = BinaGrayText)
                }
                Switch(
                    checked = widget.enabled,
                    onCheckedChange = { onToggle(index) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BinaGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = BinaGrayBorder
                    )
                )
            }
        }
    }
}

@Composable
private fun studioFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BinaAccent,
    unfocusedBorderColor = BinaGrayBorder,
    focusedLabelColor = BinaAccent,
    cursorColor = BinaAccent
)

private fun generateYaml(
    name: String,
    description: String,
    icon: String,
    systemPrompt: String,
    blockedKeywords: String,
    disclaimer: String,
    files: List<UploadedFile>,
    widgets: List<WidgetToggle>
): String {
    val id = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "my_recipe" }
    val hasCamera = widgets.any { it.id == "camera" && it.enabled }
    val hasVoice = widgets.any { it.id == "voice" && it.enabled }
    val hasLocation = widgets.any { it.id == "location" && it.enabled }
    val hasCalculator = widgets.any { it.id == "calculator" && it.enabled }

    val blocked = blockedKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val blockedYaml = if (blocked.isNotEmpty()) {
        blocked.joinToString("\n") { "    - $it" }
    } else "    - harmful"

    val knowledgeYaml = if (files.isNotEmpty()) {
        "\n# Knowledge base files (embedded locally)\n# " +
            files.joinToString("\n# ") { "${it.name} (${it.size})" } + "\n"
    } else ""

    val screens = buildString {
        // ── Home screen ──
        appendLine("  - id: home")
        appendLine("    title: \"${name.ifEmpty { "My Recipe" }}\"")
        appendLine("    body:")
        appendLine("      - text_label:")
        appendLine("          text: \"What would you like to do?\"")
        appendLine("          style: subheading")

        if (hasCamera || hasCalculator || hasLocation) {
            appendLine()
            appendLine("      - macro_grid:")
            appendLine("          columns: 2")
            appendLine("          buttons:")
            if (hasCamera) {
                appendLine("            - { label: \"📷 Camera\", action: \"go:camera\" }")
            }
            if (hasCalculator) {
                appendLine("            - { label: \"🔢 Calculator\", action: \"go:calculator\" }")
            }
            if (hasLocation) {
                appendLine("            - { label: \"📍 Nearby\", action: \"go:nearby\" }")
            }
            appendLine("            - { label: \"💬 Ask AI\", action: \"go:chat\" }")
        }

        appendLine()
        if (hasVoice) {
            appendLine("      - voice_input:")
            appendLine("          bind: user_text")
            appendLine("          hint: \"Ask anything...\"")
        } else {
            appendLine("      - text_input:")
            appendLine("          bind: user_text")
            appendLine("          hint: \"Ask anything...\"")
        }
        appendLine()
        appendLine("      - action_button:")
        appendLine("          label: \"Ask\"")
        appendLine("          action: \"ask:{{user_text}}\"")
        appendLine("          style: primary")
        appendLine()
        appendLine("      - markdown_output:")
        appendLine("          source: ai_response")
        appendLine("          streaming: true")
        appendLine("          empty_text: \"Responses will appear here...\"")

        // ── Chat screen (when multi-screen) ──
        if (hasCamera || hasCalculator || hasLocation) {
            appendLine()
            appendLine("  - id: chat")
            appendLine("    title: \"Ask AI\"")
            appendLine("    body:")
            appendLine("      - text_label:")
            appendLine("          text: \"Ask anything\"")
            appendLine("          style: subheading")
            appendLine()
            if (hasVoice) {
                appendLine("      - voice_input:")
                appendLine("          bind: user_text")
                appendLine("          hint: \"Type or speak your question...\"")
            } else {
                appendLine("      - text_input:")
                appendLine("          bind: user_text")
                appendLine("          hint: \"Type your question...\"")
            }
            appendLine()
            appendLine("      - action_button:")
            appendLine("          label: \"Ask\"")
            appendLine("          action: \"ask:{{user_text}}\"")
            appendLine("          style: primary")
            appendLine()
            appendLine("      - markdown_output:")
            appendLine("          source: ai_response")
            appendLine("          streaming: true")
            appendLine("          empty_text: \"Responses will appear here...\"")
        }

        // ── Camera screen ──
        if (hasCamera) {
            appendLine()
            appendLine("  - id: camera")
            appendLine("    title: \"Camera\"")
            appendLine("    body:")
            appendLine("      - text_label:")
            appendLine("          text: \"Take a photo for AI analysis\"")
            appendLine("          style: body")
            appendLine()
            appendLine("      - camera_input:")
            appendLine("          bind: photo_path")
            appendLine("          label: \"Take Photo\"")
            appendLine("          preview: true")
            appendLine()
            appendLine("      - text_input:")
            appendLine("          bind: user_text")
            appendLine("          hint: \"Describe what you see (optional)\"")
            appendLine("          label: \"Additional info\"")
            appendLine()
            appendLine("      - action_button:")
            appendLine("          label: \"Analyse Photo\"")
            appendLine("          action: \"vision_ask:Analyse this image. {{user_text}}\"")
            appendLine("          style: primary")
            appendLine()
            appendLine("      - markdown_output:")
            appendLine("          source: ai_response")
            appendLine("          streaming: true")
        }

        // ── Calculator screen ──
        if (hasCalculator) {
            appendLine()
            appendLine("  - id: calculator")
            appendLine("    title: \"Calculator\"")
            appendLine("    body:")
            appendLine("      - text_label:")
            appendLine("          text: \"Quick calculation\"")
            appendLine("          style: subheading")
            appendLine()
            appendLine("      - text_input:")
            appendLine("          bind: calc_a")
            appendLine("          hint: \"e.g., 1000\"")
            appendLine("          label: \"Value A\"")
            appendLine("          input_type: number")
            appendLine()
            appendLine("      - text_input:")
            appendLine("          bind: calc_b")
            appendLine("          hint: \"e.g., 500\"")
            appendLine("          label: \"Value B\"")
            appendLine("          input_type: number")
            appendLine()
            appendLine("      - slider:")
            appendLine("          bind: calc_rate")
            appendLine("          min: 0")
            appendLine("          max: 100")
            appendLine("          step: 1")
            appendLine("          label: \"Rate %\"")
            appendLine("          left_label: \"0%\"")
            appendLine("          right_label: \"100%\"")
            appendLine()
            appendLine("      - action_button:")
            appendLine("          label: \"Calculate\"")
            appendLine("          action: \"formula:calc\"")
            appendLine("          style: primary")
            appendLine()
            appendLine("      - metric_card:")
            appendLine("          source: calc_result")
            appendLine("          label: \"Result\"")
            appendLine("          format: decimal_2")
            appendLine()
            appendLine("      - action_button:")
            appendLine("          label: \"Ask AI about this result\"")
            appendLine("          action: \"ask:I calculated {{calc_a}} and {{calc_b}} with rate {{calc_rate}}%. Result: {{calc_result}}. Give advice.\"")
            appendLine("          style: secondary")
        }

        // ── Nearby screen ──
        if (hasLocation) {
            appendLine()
            appendLine("  - id: nearby")
            appendLine("    title: \"Nearby\"")
            appendLine("    body:")
            appendLine("      - text_label:")
            appendLine("          text: \"Find places near you\"")
            appendLine("          style: subheading")
            appendLine()
            appendLine("      - action_button:")
            appendLine("          label: \"Get My Location\"")
            appendLine("          action: \"geolocate\"")
            appendLine("          style: primary")
            appendLine()
            appendLine("      - geo_display:")
            appendLine("          data: places")
            appendLine("          limit: 5")
            appendLine("          show_distance: true")
            appendLine("          empty_text: \"Tap above to find nearby places\"")
            appendLine()
            appendLine("      - action_button:")
            appendLine("          label: \"Ask for directions\"")
            appendLine("          action: \"ask:How do I get to the nearest location from my coordinates {{user_location}}?\"")
            appendLine("          style: secondary")
        }
    }

    val variables = buildString {
        appendLine("  user_text:    { type: string, default: \"\" }")
        appendLine("  ai_response:  { type: string, default: \"\" }")
        if (hasCamera) appendLine("  photo_path:   { type: string, default: \"\" }")
        if (hasCalculator) {
            appendLine("  calc_a:       { type: number, default: \"0\" }")
            appendLine("  calc_b:       { type: number, default: \"0\" }")
            appendLine("  calc_rate:    { type: number, default: \"10\" }")
            appendLine("  calc_result:  { type: number, default: \"0\" }")
        }
    }

    val formulas = if (hasCalculator) {
        """
formulas:
  calc:
    expression: "({{calc_a}} + {{calc_b}}) * {{calc_rate}} / 100"
    output: calc_result
"""
    } else ""

    val data = if (hasLocation) {
        """
data:
  places:
    type: points
    items:
      - { name: "Sample Location 1", lat: 3.139, lng: 101.687, info: "Edit in YAML" }
      - { name: "Sample Location 2", lat: 3.145, lng: 101.710, info: "Edit in YAML" }
      - { name: "Sample Location 3", lat: 3.120, lng: 101.660, info: "Edit in YAML" }
"""
    } else ""

    val permissions = buildList {
        if (hasCamera) add("  - camera")
        if (hasLocation) add("  - location")
    }.joinToString("\n")

    val escapedPrompt = systemPrompt.replace("\"", "\\\"")
    val systemPromptYaml = if (escapedPrompt.isBlank()) {
        "  system_prompt: \"You are a helpful assistant.\""
    } else {
        "  system_prompt: |\n    $escapedPrompt"
    }

    return """id: $id
name: "${name.ifEmpty { "My Recipe" }}"
description: "${description.ifEmpty { "A custom AI recipe" }}"
icon: "$icon"
version: "1.0.0"
category: Custom
$knowledgeYaml
author:
  name: User
  organisation: ""
  verified: false

model:
  model_id: gemma-4-e2b-it
  backend: cpu
$systemPromptYaml

theme:
  primary: "#C45A3A"
  secondary: "#E8DDD3"

variables:
$variables
screens:
$screens
$formulas$data
safety:
  blocked_keywords:
$blockedYaml
  escalation_message: "This request has been blocked for safety."
  disclaimer: "${disclaimer.ifEmpty { "AI-generated content." }}"

permissions:
${permissions.ifEmpty { "  []" }}
"""
}
