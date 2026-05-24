package com.spendwise.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.spendwise.ui.SettingsUiState

@Composable
internal actual fun DataTransferSettings(
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    var pendingExport by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val csv = pendingExport ?: return@rememberLauncherForActivityResult
        pendingExport = null
        if (uri == null) return@rememberLauncherForActivityResult
        val result = runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8).use { writer ->
                requireNotNull(writer) { "Unable to open export file" }
                writer.write(csv)
            }
        }
        viewModel.showMessage(
            result.fold(
                onSuccess = { "CSV exported" },
                onFailure = { "CSV export failed" }
            )
        )
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val csvText = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8).use { reader ->
                requireNotNull(reader) { "Unable to open import file" }
                reader.readText()
            }
        }
        csvText.fold(
            onSuccess = { viewModel.importCsv(it) },
            onFailure = { viewModel.showMessage("CSV import failed") }
        )
    }

    SettingsRow(
        title = "Export CSV",
        subtitle = "${state.expenses.size} expenses",
        onClick = {
            pendingExport = viewModel.exportCsv()
            exportLauncher.launch("spendwise_backup.csv")
        }
    )
    SettingsRow(
        title = "Import CSV",
        subtitle = "date, amount, currency, category, note",
        onClick = {
            importLauncher.launch(
                arrayOf(
                    "text/csv",
                    "text/comma-separated-values",
                    "text/plain",
                    "application/csv",
                    "application/vnd.ms-excel"
                )
            )
        }
    )
}
