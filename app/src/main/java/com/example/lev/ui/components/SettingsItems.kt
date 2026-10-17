package com.example.lev.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Linha de seção de preferências (Configurações) com a divisória M3 ao final.
 * O título usa `titleMedium` do tema (SemiBold), sem peso de fonte inline.
 */
@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        content()
        HorizontalDivider()
    }
}

/**
 * Linha de preferência: ícone decorativo, título com descrição e controle à direita.
 * Alvo de toque de 48dp garantido pelo padding de 16dp + altura da linha.
 */
@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    description: String,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing()
    }
}
