package com.example.lev.ui.screens.planning

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lev.ui.screens.budget.BudgetScreen
import com.example.lev.ui.screens.budget.BudgetViewModel
import com.example.lev.ui.screens.goals.GoalsScreen
import com.example.lev.ui.screens.goals.GoalsViewModel
import com.example.lev.ui.screens.recurring.RecurrencesScreen
import com.example.lev.ui.screens.recurring.RecurrencesViewModel

private val planningTabs = listOf("Orçamento", "Metas", "Recorrências")

/**
 * Planning hub: YNAB-style budgets, goals and recurring bills in one place,
 * following the pattern of Mobills/Organizze "Planejamento" sections.
 */
@Composable
fun PlanningScreen(
    budgetViewModel: BudgetViewModel,
    goalsViewModel: GoalsViewModel,
    recurrencesViewModel: RecurrencesViewModel,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            planningTabs.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = planningTabs.size),
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }

        when (selectedTab) {
            0 -> BudgetScreen(viewModel = budgetViewModel, modifier = Modifier.fillMaxSize())
            1 -> GoalsScreen(viewModel = goalsViewModel, modifier = Modifier.fillMaxSize())
            else -> RecurrencesScreen(viewModel = recurrencesViewModel, modifier = Modifier.fillMaxSize())
        }
    }
}
