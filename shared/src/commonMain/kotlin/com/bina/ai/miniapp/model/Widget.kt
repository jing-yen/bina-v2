package com.bina.ai.miniapp.model

import kotlinx.serialization.Serializable

@Serializable(with = WidgetSerializer::class)
sealed class Widget {

    abstract val visibleIf: String?
    abstract val hiddenIf: String?

    @Serializable
    data class TextLabel(
        val text: String = "",
        val style: String = "body",
        val align: String = "left",
        val color: String = "",
        override val visibleIf: String? = null,
        override val hiddenIf: String? = null
    ) : Widget()

    @Serializable
    data class TextInput(
        val bind: String,
        val hint: String = "",
        val label: String = "",
        val inputType: String = "text",
        val options: List<String> = emptyList(),
        override val visibleIf: String? = null,
        override val hiddenIf: String? = null
    ) : Widget()

    @Serializable
    data class VoiceInput(
        val bind: String,
        val hint: String = "",
        val language: String = "",
        val mode: String = "tap",
        override val visibleIf: String? = null,
        override val hiddenIf: String? = null
    ) : Widget()

    @Serializable
    data class CameraInput(
        val bind: String,
        val label: String = "Take Photo",
        val preview: Boolean = true,
        override val visibleIf: String? = null,
        override val hiddenIf: String? = null
    ) : Widget()

    @Serializable
    data class MacroGrid(
        val columns: Int = 2,
        val buttons: List<GridButton> = emptyList(),
        override val visibleIf: String? = null,
        override val hiddenIf: String? = null
    ) : Widget()

    @Serializable
    data class Slider(
        val bind: String,
        val min: Float = 0f,
        val max: Float = 100f,
        val step: Float = 1f,
        val label: String = "",
        val leftLabel: String = "",
        val rightLabel: String = "",
        val showValue: Boolean = true,
        override val visibleIf: String? = null,
        override val hiddenIf: String? = null
    ) : Widget()

    @Serializable
    data class ActionButton(
        val label: String,
        val action: String,
        val style: String = "primary",
        val icon: String = "",
        val confirm: String = "",
        override val visibleIf: String? = null,
        override val hiddenIf: String? = null
    ) : Widget()

    @Serializable
    data class MarkdownOutput(
        val source: String,
        val emptyText: String = "",
        val streaming: Boolean = true,
        override val visibleIf: String? = null,
        override val hiddenIf: String? = null
    ) : Widget()

    @Serializable
    data class MetricCard(
        val source: String,
        val label: String = "",
        val prefix: String = "",
        val suffix: String = "",
        val color: String = "",
        val format: String = "decimal_2",
        override val visibleIf: String? = null,
        override val hiddenIf: String? = null
    ) : Widget()

    @Serializable
    data class GeoDisplay(
        val data: String,
        val limit: Int = 5,
        val showDistance: Boolean = true,
        val emptyText: String = "",
        override val visibleIf: String? = null,
        override val hiddenIf: String? = null
    ) : Widget()

    @Serializable
    data class ProgressBar(
        val bind: String,
        val total: Int = 3,
        override val visibleIf: String? = null,
        override val hiddenIf: String? = null
    ) : Widget()

    @Serializable
    data class ChecklistItems(
        val bind: String,
        val items: List<ChecklistItem> = emptyList(),
        override val visibleIf: String? = null,
        override val hiddenIf: String? = null
    ) : Widget()
}
