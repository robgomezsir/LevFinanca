package com.example.lev.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.lev.data.repository.SyncChipState

/** Chip de estado do sync (M7 §C): sincronizado, pendente, offline, erro. */
@Composable
fun SyncStatusChip(
    chip: SyncChipState,
    pendingCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    val (text, container, content) =
        when (chip) {
            SyncChipState.SYNCED ->
                Triple(
                    "Sincronizado",
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                )
            SyncChipState.PENDING ->
                Triple(
                    if (pendingCount > 0) "Pendente ($pendingCount)" else "Pendente",
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                )
            SyncChipState.OFFLINE ->
                Triple(
                    "Offline",
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            SyncChipState.ERROR ->
                Triple(
                    "Erro",
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer,
                )
            SyncChipState.SYNCING ->
                Triple(
                    "Sincronizando…",
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                )
            SyncChipState.NEVER ->
                Triple(
                    "Não configurado",
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
        }
    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .background(container)
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = content)
    }
}

@Composable
fun SyncStatusChip(
    statusText: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
