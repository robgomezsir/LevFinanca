package com.example.lev.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector

data class NavItem(
    val label: String,
    val icon: ImageVector? = null,
    val iconRes: Int? = null,
    val route: String,
    val badgeCount: Int = 0,
)

/**
 * Destinos da barra inferior: só o que não tem outro ponto de entrada.
 *
 * Contas sai daqui porque o painel tem o bloco "Outras contas" com "Ver contas"; Categorias e
 * Configurações saem porque são alcançadas por Configurações e pela engrenagem do cabeçalho.
 */
fun bottomNavItems(): List<NavItem> =
    listOf(
        NavItem(
            label = "Início",
            icon = Icons.Default.Home,
            route = "dashboard",
        ),
        NavItem(
            label = "Lançamentos",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            route = "transactions",
        ),
        NavItem(
            label = "Orçamento",
            icon = Icons.Default.PieChart,
            route = "planning",
        ),
        NavItem(
            label = "Relatórios",
            icon = Icons.Default.BarChart,
            route = "reports",
        ),
    )
