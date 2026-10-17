package com.example.lev.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lev.ui.theme.LevTheme

@Composable
fun AmountKeypad(
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val rows =
            listOf(
                listOf("7", "8", "9", "⌫"),
                listOf("4", "5", "6", "+"),
                listOf("1", "2", "3", "-"),
                listOf(",", "0", "OK", ""),
            )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    KeypadButton(
                        text = key,
                        modifier = Modifier.weight(1f).aspectRatio(1.5f),
                        onClick = {
                            when (key) {
                                "⌫" -> onBackspace()
                                "OK" -> onSubmit()
                                "+", "-" -> onOperator(key)
                                "" -> {} // Empty space placeholder
                                else -> onDigit(key)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (text.isEmpty()) {
        Spacer(modifier = modifier)
        return
    }

    val keyKind = keyKindOf(text)
    val bgColor = keyBackground(keyKind)
    val contentColor = keyContentColor(keyKind)

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .heightIn(min = 56.dp)
                .clip(MaterialTheme.shapes.large)
                .background(bgColor)
                .clickable(
                    onClickLabel = if (text == "OK") "Salvar lançamento" else text,
                    onClick = onClick,
                ),
    ) {
        KeypadKeyContent(text = text, kind = keyKind, contentColor = contentColor)
    }
}

private enum class KeyKind { Confirm, Erase, Operator, Digit }

private fun keyKindOf(text: String): KeyKind =
    when (text) {
        "OK" -> KeyKind.Confirm
        "⌫" -> KeyKind.Erase
        "+", "-" -> KeyKind.Operator
        else -> KeyKind.Digit
    }

@Composable
private fun keyBackground(kind: KeyKind): androidx.compose.ui.graphics.Color =
    when (kind) {
        KeyKind.Confirm -> MaterialTheme.colorScheme.primary
        KeyKind.Operator -> MaterialTheme.colorScheme.secondaryContainer
        KeyKind.Erase, KeyKind.Digit -> MaterialTheme.colorScheme.surfaceVariant
    }

@Composable
private fun keyContentColor(kind: KeyKind): androidx.compose.ui.graphics.Color =
    when (kind) {
        KeyKind.Confirm -> MaterialTheme.colorScheme.onPrimary
        KeyKind.Operator -> MaterialTheme.colorScheme.onSecondaryContainer
        KeyKind.Erase, KeyKind.Digit -> MaterialTheme.colorScheme.onSurface
    }

@Composable
private fun KeypadKeyContent(
    text: String,
    kind: KeyKind,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    when (kind) {
        KeyKind.Erase -> {
            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Apagar", tint = contentColor)
        }
        KeyKind.Confirm -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = contentColor)
                Text(
                    text = "Salvar",
                    color = contentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        KeyKind.Operator, KeyKind.Digit -> {
            Text(
                text = text,
                color = contentColor,
                fontSize = 24.sp,
                fontWeight = if (kind == KeyKind.Operator) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Preview
@Composable
fun KeypadPreview() {
    LevTheme {
        AmountKeypad({}, {}, {}, {})
    }
}
