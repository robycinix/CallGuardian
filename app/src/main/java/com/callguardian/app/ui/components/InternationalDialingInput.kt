package com.callguardian.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.callguardian.app.core.model.CountryDialingInfo
import com.callguardian.app.core.model.SupportedCountries
import com.callguardian.app.telephony.DeviceCountryResolver

@Composable
fun rememberDefaultDialingCountry(): CountryDialingInfo {
    val context = LocalContext.current
    return remember(context) { DeviceCountryResolver.defaultDialingCountry(context) }
}

@Composable
fun InternationalDialingInput(
    value: String,
    onValueChange: (String) -> Unit,
    selectedCountry: CountryDialingInfo,
    onCountrySelected: (CountryDialingInfo) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    allowAnonymous: Boolean = false,
) {
    var pickerOpen by remember { mutableStateOf(false) }

    if (pickerOpen) {
        CountryDialingPickerDialog(
            selectedCountry = selectedCountry,
            onCountrySelected = { country ->
                onCountrySelected(country)
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false },
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CountryDialCodeButton(
            country = selectedCountry,
            onClick = { pickerOpen = true },
            modifier = Modifier.padding(top = 8.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            supportingText = if (supportingText != null) {
                { Text(supportingText) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (allowAnonymous) KeyboardType.Text else KeyboardType.Phone,
            ),
            singleLine = true,
        )
    }
}

@Composable
private fun CountryDialCodeButton(
    country: CountryDialingInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .widthIn(min = 106.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(country.flag.ifBlank { country.iso }, maxLines = 1)
            Text(
                text = country.dialCode,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
    }
}

@Composable
private fun CountryDialingPickerDialog(
    selectedCountry: CountryDialingInfo,
    onCountrySelected: (CountryDialingInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val countries = SupportedCountries
    val filteredCountries = remember(query, countries) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            countries
        } else {
            countries.filter { it.matchesDialingQuery(cleanQuery) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prefisso internazionale") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cerca paese o prefisso") },
                    placeholder = { Text("Italia, IT, +39") },
                    singleLine = true,
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filteredCountries, key = { it.iso }) { country ->
                        CountryDialingOption(
                            country = country,
                            selected = country.iso == selectedCountry.iso,
                            onClick = { onCountrySelected(country) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        },
    )
}

@Composable
private fun CountryDialingOption(
    country: CountryDialingInfo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(country.flag.ifBlank { country.iso })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = country.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${country.iso} - ${country.dialCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun CountryDialingInfo.matchesDialingQuery(query: String): Boolean {
    val cleanQuery = query.trim()
    val cleanDigits = cleanQuery.filter(Char::isDigit)
    return name.contains(cleanQuery, ignoreCase = true) ||
        iso.contains(cleanQuery, ignoreCase = true) ||
        dialCode.contains(cleanQuery) ||
        (cleanDigits.isNotBlank() && dialCode.filter(Char::isDigit).contains(cleanDigits))
}
