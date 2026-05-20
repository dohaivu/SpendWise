package com.spendwise.ui.settings

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

internal sealed interface SettingsRoute : NavKey {
    @Serializable
    data object Home : SettingsRoute

    @Serializable
    data object AnnualReport : SettingsRoute

    @Serializable
    data object CategoryList : SettingsRoute

    @Serializable
    data object CategoryEditor : SettingsRoute

    @Serializable
    data object TagUsage : SettingsRoute
}
