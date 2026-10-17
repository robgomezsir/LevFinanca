package com.example.lev.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.lev.domain.MoneyFormatter
import com.example.lev.ui.theme.ExpenseDark
import com.example.lev.ui.theme.ExpenseLight
import com.example.lev.ui.theme.IncomeDark
import com.example.lev.ui.theme.IncomeLight
import com.example.lev.ui.theme.LevTheme

@Composable
fun MoneyText(
    cents: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    useSemanticColor: Boolean = true,
    showSign: Boolean = false,
) {
    val formatted = MoneyFormatter.format(cents)
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val color =
        if (useSemanticColor) {
            when {
                cents < 0 -> if (dark) ExpenseDark else ExpenseLight
                cents > 0 && showSign -> if (dark) IncomeDark else IncomeLight
                cents > 0 -> if (dark) IncomeDark else IncomeLight
                else -> Color.Unspecified
            }
        } else {
            Color.Unspecified
        }
    // Sinal explícito para despesa: nunca só cor (Base §7)
    val text =
        if (showSign && cents != 0L) {
            val sign = if (cents < 0) "−" else "+"
            "$sign ${formatted.replace("-", "").trim()}"
        } else if (cents < 0) {
            formatted.replace("-", "−")
        } else {
            formatted
        }

    Text(
        text = text,
        modifier = modifier.semantics { contentDescription = MoneyFormatter.format(cents) },
        style = style.copy(fontFeatureSettings = "tnum"),
        color = color,
        textAlign = TextAlign.End,
    )
}

@Preview(showBackground = true)
@Composable
fun MoneyTextPreview() {
    LevTheme {
        MoneyText(cents = 150000)
    }
}

@Preview(showBackground = true)
@Composable
fun MoneyTextNegativePreview() {
    LevTheme {
        MoneyText(cents = -5000)
    }
}
