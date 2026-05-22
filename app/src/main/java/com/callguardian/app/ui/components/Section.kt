package com.callguardian.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        colors.primary.copy(alpha = 0.34f),
                        colors.tertiary.copy(alpha = 0.18f),
                        colors.secondary.copy(alpha = 0.26f),
                    ),
                ),
                shape = MaterialTheme.shapes.small,
            ),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(1.dp),
            shape = MaterialTheme.shapes.small,
            colors = CardDefaults.cardColors(
                containerColor = lerp(colors.surface, colors.primaryContainer, 0.24f).copy(alpha = 0.98f),
                contentColor = colors.onSurface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.18f)),
        ) {
            Box(
                Modifier.background(
                    Brush.linearGradient(
                        listOf(
                            colors.primary.copy(alpha = 0.055f),
                            Color.Transparent,
                            colors.secondary.copy(alpha = 0.042f),
                        ),
                    ),
                ),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        trailing?.invoke()
                    }
                    content()
                }
            }
        }
    }
}
