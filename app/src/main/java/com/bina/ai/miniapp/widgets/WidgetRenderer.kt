package com.bina.ai.miniapp.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.bina.ai.inference.InferenceEngine
import com.bina.ai.miniapp.model.DataSet
import com.bina.ai.miniapp.model.Widget
import com.bina.ai.miniapp.runtime.VariableStore

@Composable
fun RenderWidget(
    widget: Widget,
    store: VariableStore,
    themeColor: Color,
    isLoading: Boolean,
    dataSets: Map<String, DataSet>,
    onAction: (String) -> Unit,
    inferenceEngine: InferenceEngine? = null
) {
    if (widget.visibleIf != null && !store.isTrue(widget.visibleIf!!)) return
    if (widget.hiddenIf != null && store.isTrue(widget.hiddenIf!!)) return

    when (widget) {
        is Widget.TextLabel -> TextLabelWidget(widget, store, themeColor)
        is Widget.TextInput -> TextInputWidget(widget, store, themeColor)
        is Widget.VoiceInput -> VoiceInputWidget(widget, store, themeColor, inferenceEngine)
        is Widget.CameraInput -> CameraInputWidget(widget, store, themeColor)
        is Widget.MacroGrid -> MacroGridWidget(widget, themeColor, onAction)
        is Widget.Slider -> SliderWidget(widget, store, themeColor)
        is Widget.ActionButton -> ActionButtonWidget(widget, store, themeColor, isLoading, onAction)
        is Widget.MarkdownOutput -> MarkdownOutputWidget(widget, store, themeColor)
        is Widget.MetricCard -> MetricCardWidget(widget, store, themeColor)
        is Widget.GeoDisplay -> GeoDisplayWidget(widget, store, dataSets[widget.data], themeColor)
        is Widget.ProgressBar -> ProgressBarWidget(widget, store, themeColor)
        is Widget.ChecklistItems -> ChecklistItemsWidget(widget, store, themeColor)
    }
}
