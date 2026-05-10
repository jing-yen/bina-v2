package com.bina.ai.install.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InstallRecord(
    val recipeId: String,
    @SerialName("installed_at") val installedAt: Long,
    @SerialName("enabled_features") val enabledFeatureIds: Set<String>
)
