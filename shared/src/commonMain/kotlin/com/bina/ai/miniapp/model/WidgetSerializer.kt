package com.bina.ai.miniapp.model

import com.charleskorn.kaml.YamlInput
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.yamlMap
import com.charleskorn.kaml.yamlScalar
import com.charleskorn.kaml.yamlList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object WidgetSerializer : KSerializer<Widget> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Widget")

    override fun serialize(encoder: Encoder, value: Widget) {
        throw UnsupportedOperationException("Widget serialization not needed")
    }

    override fun deserialize(decoder: Decoder): Widget {
        val input = decoder as YamlInput
        val map = input.node.yamlMap

        val entry = map.entries.entries.first()
        val widgetType = entry.key.content
        val props = entry.value.yamlMap

        return when (widgetType) {
            "text_label" -> Widget.TextLabel(
                text = props.string("text"),
                style = props.stringOr("style", "body"),
                align = props.stringOr("align", "left"),
                color = props.stringOr("color", ""),
                visibleIf = props.stringOrNull("visible_if"),
                hiddenIf = props.stringOrNull("hidden_if")
            )
            "text_input" -> Widget.TextInput(
                bind = props.string("bind"),
                hint = props.stringOr("hint", ""),
                label = props.stringOr("label", ""),
                inputType = props.stringOr("input_type", "text"),
                options = props.stringList("options"),
                visibleIf = props.stringOrNull("visible_if"),
                hiddenIf = props.stringOrNull("hidden_if")
            )
            "voice_input" -> Widget.VoiceInput(
                bind = props.string("bind"),
                hint = props.stringOr("hint", ""),
                language = props.stringOr("language", ""),
                mode = props.stringOr("mode", "tap"),
                visibleIf = props.stringOrNull("visible_if"),
                hiddenIf = props.stringOrNull("hidden_if")
            )
            "camera_input" -> Widget.CameraInput(
                bind = props.string("bind"),
                label = props.stringOr("label", "Take Photo"),
                preview = props.boolOr("preview", true),
                visibleIf = props.stringOrNull("visible_if"),
                hiddenIf = props.stringOrNull("hidden_if")
            )
            "macro_grid" -> Widget.MacroGrid(
                columns = props.intOr("columns", 2),
                buttons = props.buttonList("buttons"),
                visibleIf = props.stringOrNull("visible_if"),
                hiddenIf = props.stringOrNull("hidden_if")
            )
            "slider" -> Widget.Slider(
                bind = props.string("bind"),
                min = props.floatOr("min", 0f),
                max = props.floatOr("max", 100f),
                step = props.floatOr("step", 1f),
                label = props.stringOr("label", ""),
                leftLabel = props.stringOr("left_label", ""),
                rightLabel = props.stringOr("right_label", ""),
                showValue = props.boolOr("show_value", true),
                visibleIf = props.stringOrNull("visible_if"),
                hiddenIf = props.stringOrNull("hidden_if")
            )
            "action_button" -> Widget.ActionButton(
                label = props.string("label"),
                action = props.string("action"),
                style = props.stringOr("style", "primary"),
                icon = props.stringOr("icon", ""),
                confirm = props.stringOr("confirm", ""),
                visibleIf = props.stringOrNull("visible_if"),
                hiddenIf = props.stringOrNull("hidden_if")
            )
            "markdown_output" -> Widget.MarkdownOutput(
                source = props.string("source"),
                emptyText = props.stringOr("empty_text", ""),
                streaming = props.boolOr("streaming", true),
                visibleIf = props.stringOrNull("visible_if"),
                hiddenIf = props.stringOrNull("hidden_if")
            )
            "metric_card" -> Widget.MetricCard(
                source = props.string("source"),
                label = props.stringOr("label", ""),
                prefix = props.stringOr("prefix", ""),
                suffix = props.stringOr("suffix", ""),
                color = props.stringOr("color", ""),
                format = props.stringOr("format", "decimal_2"),
                visibleIf = props.stringOrNull("visible_if"),
                hiddenIf = props.stringOrNull("hidden_if")
            )
            "geo_display" -> Widget.GeoDisplay(
                data = props.string("data"),
                limit = props.intOr("limit", 5),
                showDistance = props.boolOr("show_distance", true),
                emptyText = props.stringOr("empty_text", ""),
                visibleIf = props.stringOrNull("visible_if"),
                hiddenIf = props.stringOrNull("hidden_if")
            )
            "progress_bar" -> Widget.ProgressBar(
                bind = props.string("bind"),
                total = props.intOr("total", 3),
                visibleIf = props.stringOrNull("visible_if"),
                hiddenIf = props.stringOrNull("hidden_if")
            )
            "checklist_items" -> Widget.ChecklistItems(
                bind = props.string("bind"),
                items = props.checklistItemList("items"),
                visibleIf = props.stringOrNull("visible_if"),
                hiddenIf = props.stringOrNull("hidden_if")
            )
            else -> Widget.TextLabel(text = "Unknown widget: $widgetType")
        }
    }
}

private fun YamlMap.string(key: String): String =
    get<YamlScalar>(key)?.content ?: ""

private fun YamlMap.stringOr(key: String, default: String): String =
    getScalar(key)?.content ?: default

private fun YamlMap.stringOrNull(key: String): String? =
    getScalar(key)?.content

private fun YamlMap.intOr(key: String, default: Int): Int =
    getScalar(key)?.content?.toIntOrNull() ?: default

private fun YamlMap.floatOr(key: String, default: Float): Float =
    getScalar(key)?.content?.toFloatOrNull() ?: default

private fun YamlMap.boolOr(key: String, default: Boolean): Boolean =
    getScalar(key)?.content?.toBooleanStrictOrNull() ?: default

private fun YamlMap.getScalar(key: String): YamlScalar? =
    try { get<YamlScalar>(key) } catch (_: Exception) { null }

private fun YamlMap.stringList(key: String): List<String> {
    val list = try { get<YamlList>(key) } catch (_: Exception) { return emptyList() }
        ?: return emptyList()
    return list.items.mapNotNull { node ->
        (node as? YamlScalar)?.content
    }
}

private fun YamlMap.checklistItemList(key: String): List<ChecklistItem> {
    val list = try { get<YamlList>(key) } catch (_: Exception) { return emptyList() }
        ?: return emptyList()
    return list.items.mapNotNull { node ->
        val itemMap = (node as? YamlMap) ?: return@mapNotNull null
        ChecklistItem(
            label = itemMap.string("label"),
            type = itemMap.stringOr("type", "text")
        )
    }
}

private fun YamlMap.buttonList(key: String): List<GridButton> {
    val list = try { get<YamlList>(key) } catch (_: Exception) { return emptyList() }
        ?: return emptyList()
    return list.items.mapNotNull { node ->
        val btnMap = (node as? YamlMap) ?: return@mapNotNull null
        GridButton(
            label = btnMap.string("label"),
            action = btnMap.string("action"),
            color = btnMap.stringOr("color", ""),
            icon = btnMap.stringOr("icon", "")
        )
    }
}
