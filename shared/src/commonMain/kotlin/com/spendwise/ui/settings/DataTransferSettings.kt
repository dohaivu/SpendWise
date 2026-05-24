package com.spendwise.ui.settings

import androidx.compose.runtime.Composable
import com.spendwise.ui.SettingsUiState

@Composable
internal expect fun DataTransferSettings(
    state: SettingsUiState,
    viewModel: SettingsViewModel
)
