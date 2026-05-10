package com.bina.ai.miniapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MiniApp(
    val id: String,
    val name: String,
    val description: String = "",
    val icon: String = "",
    val version: String = "1.0.0",
    val category: String = "",
    // Hub-facing metadata. All optional with safe defaults so old YAMLs keep parsing
    // and Studio's generateYaml() can adopt these fields incrementally.
    @SerialName("cover_image") val coverImage: String = "",
    val featured: Boolean = false,
    val emergency: Boolean = false,
    val dialect: String = "",
    val tags: List<String> = emptyList(),
    val author: Author = Author(),
    val model: ModelConfig = ModelConfig(),
    val theme: ThemeConfig = ThemeConfig(),
    val localisation: LocalisationConfig = LocalisationConfig(),
    val variables: Map<String, VariableDef> = emptyMap(),
    val screens: List<MiniAppScreen> = emptyList(),
    val formulas: Map<String, FormulaDef> = emptyMap(),
    val data: Map<String, DataSet> = emptyMap(),
    val safety: SafetyConfig = SafetyConfig(),
    val permissions: List<String> = emptyList()
)

@Serializable
data class Author(
    val name: String = "",
    val organisation: String = "",
    val verified: Boolean = false
)

@Serializable
data class ModelConfig(
    @SerialName("model_id") val modelId: String = "gemma-4-e2b-it",
    val backend: String = "cpu",
    @SerialName("system_prompt") val systemPrompt: String = "",
    val sampler: SamplerConfig = SamplerConfig()
)

@Serializable
data class SamplerConfig(
    val temperature: Float = 0.3f,
    @SerialName("top_k") val topK: Int = 40,
    @SerialName("top_p") val topP: Float = 0.95f,
    @SerialName("max_tokens") val maxTokens: Int = 512
)

@Serializable
data class ThemeConfig(
    val primary: String = "#091A7A",
    val secondary: String = "#1E3A8A",
    @SerialName("text_size") val textSize: String = "standard"
)

@Serializable
data class LocalisationConfig(
    @SerialName("default_language") val defaultLanguage: String = "en",
    val labels: Map<String, Map<String, String>> = emptyMap()
)

@Serializable
data class VariableDef(
    val type: String = "string",
    val default: String = ""
)

@Serializable
data class MiniAppScreen(
    val id: String,
    val title: String = "",
    val body: List<Widget> = emptyList()
)

@Serializable
data class FormulaDef(
    val expression: String,
    val output: String
)

@Serializable
data class DataSet(
    val type: String = "points",
    val items: List<DataPoint> = emptyList()
)

@Serializable
data class DataPoint(
    val name: String,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val info: String = ""
)

@Serializable
data class SafetyConfig(
    @SerialName("blocked_keywords") val blockedKeywords: List<String> = emptyList(),
    @SerialName("escalation_message") val escalationMessage: String = "",
    val disclaimer: String = ""
)

@Serializable
data class GridButton(
    val label: String,
    val action: String,
    val color: String = ""
)
