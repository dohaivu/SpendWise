package com.spendwise.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.spendwise.ui.supportedCurrencies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurrencyMenu(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Currency"
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            supportedCurrencies.forEach { currency ->
                val format = currencyDisplayFormat(currency)
                DropdownMenuItem(
                    text = { Text("${format.symbol} $currency") },
                    onClick = {
                        expanded = false
                        onSelected(currency)
                    }
                )
            }
        }
    }
}
