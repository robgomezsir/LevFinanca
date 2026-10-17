package com.example.lev.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lev.LevApplication
import com.example.lev.data.model.Account
import com.example.lev.data.model.Category
import com.example.lev.ui.AppViewModelProvider
import com.example.lev.ui.components.LocalAppearance
import com.example.lev.ui.components.LocalIsDark
import com.example.lev.ui.components.bottomNavItems
import com.example.lev.ui.components.brandSurfaceBrush
import com.example.lev.ui.screens.accounts.AccountsScreen
import com.example.lev.ui.screens.accounts.AccountsViewModel
import com.example.lev.ui.screens.archive.ArchiveScreen
import com.example.lev.ui.screens.budget.BudgetViewModel
import com.example.lev.ui.screens.cards.CardsScreen
import com.example.lev.ui.screens.categories.CategoriesScreen
import com.example.lev.ui.screens.categories.CategoriesViewModel
import com.example.lev.ui.screens.dashboard.DashboardScreen
import com.example.lev.ui.screens.dashboard.DashboardViewModel
import com.example.lev.ui.screens.goals.GoalsViewModel
import com.example.lev.ui.screens.importexport.ImportExportScreen
import com.example.lev.ui.screens.invoices.InvoiceDetailsScreen
import com.example.lev.ui.screens.networth.NetWorthScreen
import com.example.lev.ui.screens.planning.PlanningScreen
import com.example.lev.ui.screens.recurring.RecurrencesViewModel
import com.example.lev.ui.screens.reports.ReportsScreen
import com.example.lev.ui.screens.reports.ReportsViewModel
import com.example.lev.ui.screens.settings.SettingsScreen
import com.example.lev.ui.screens.settings.SettingsViewModel
import com.example.lev.ui.screens.sync.SyncSettingsScreen
import com.example.lev.ui.screens.sync.SyncViewModel
import com.example.lev.ui.screens.transactions.FastEntryBottomSheet
import com.example.lev.ui.screens.transactions.TransactionListScreen
import com.example.lev.ui.screens.transactions.TransactionsViewModel
import com.example.lev.ui.theme.Appearance
import com.example.lev.ui.theme.GlassEdgeDark
import com.example.lev.ui.theme.GlassEdgeLight
import com.example.lev.ui.theme.GlassVeilDark
import com.example.lev.ui.theme.GlassVeilLight
import com.example.lev.ui.theme.PoiretOne
import kotlinx.coroutines.launch

private data class DrawerEntry(val label: String, val route: String)

private val drawerEntries =
    listOf(
        DrawerEntry("Contas", "accounts"),
        DrawerEntry("Cartões", "cards"),
        DrawerEntry("Categorias", "categories"),
        DrawerEntry("Patrimônio", "networth"),
        DrawerEntry("Arquivo", "archive"),
        DrawerEntry("Lixeira", "trash"),
        DrawerEntry("Importar / Exportar", "importexport"),
        DrawerEntry("Configurações", "settings"),
    )

private val bottomRoutes = setOf("dashboard", "transactions", "planning", "reports")

/** Estado do chrome em modo vidro: véu e borda resolvem claro/escuro uma única vez. */
private data class GlassChrome(val glass: Boolean, val veil: Color, val edge: Color)

@Composable
private fun rememberGlassChrome(): GlassChrome {
    val dark = LocalIsDark.current
    return GlassChrome(
        glass = LocalAppearance.current == Appearance.GLASS,
        veil = if (dark) GlassVeilDark else GlassVeilLight,
        edge = if (dark) GlassEdgeDark else GlassEdgeLight,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val navItems = bottomNavItems()
    // Modo vidro também no chrome do app (barra inferior e gaveta), não só nos cards.
    val chrome = rememberGlassChrome()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Entrada rápida única (Base §6 FAB "Novo"): o ViewModel é içado para o escopo do
    // MainScreen para sobreviver à navegação entre abas, e o sheet é renderizado uma
    // única vez aqui — nenhuma tela filha pode ter FAB próprio de lançamento.
    val transactionsViewModel: TransactionsViewModel =
        viewModel(factory = AppViewModelProvider.Factory)
    val fastEntryAccounts by transactionsViewModel.accounts.collectAsStateWithLifecycle()
    val fastEntryCategories by transactionsViewModel.categories.collectAsStateWithLifecycle()
    var showFastEntry by rememberSaveable { mutableStateOf(false) }

    fun navigateSingleTop(route: String) {
        if (currentRoute != route) {
            navController.navigate(route) {
                launchSingleTop = true
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                restoreState = true
            }
        }
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Corpo extraído para os dois ramos: esta versão do M3 parametriza
            // ModalDrawerSheet direto (drawerContainerColor), sem objeto de cores.
            @Composable
            fun drawerBody() {
                Text(
                    text = "leve",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = PoiretOne,
                    modifier = Modifier.padding(20.dp),
                )
                drawerEntries.forEach { entry ->
                    NavigationDrawerItem(
                        label = { Text(entry.label) },
                        selected = currentRoute == entry.route,
                        onClick = { navigateSingleTop(entry.route) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
            if (chrome.glass) {
                ModalDrawerSheet(drawerContainerColor = chrome.veil) { drawerBody() }
            } else {
                ModalDrawerSheet { drawerBody() }
            }
        },
        modifier = modifier,
    ) {
        Scaffold(
            topBar = {
                Box(modifier = Modifier.background(brandSurfaceBrush())) {
                    CompositionLocalProvider(LocalContentColor provides Color.White) {
                        TopAppBar(
                            title = { Text("leve", fontFamily = PoiretOne, color = Color.White) },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Abrir menu", tint = Color.White)
                                }
                            },
                            actions = {
                                IconButton(onClick = { navigateSingleTop("settings") }) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Configurações",
                                        tint = Color.White,
                                    )
                                }
                            },
                            colors =
                                TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                    navigationIconContentColor = Color.White,
                                    titleContentColor = Color.White,
                                    actionIconContentColor = Color.White,
                                ),
                        )
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor =
                        if (chrome.glass) chrome.veil else NavigationBarDefaults.containerColor,
                    modifier =
                        if (chrome.glass) {
                            Modifier.drawBehind {
                                drawLine(
                                    color = chrome.edge,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            }
                        } else {
                            Modifier
                        },
                ) {
                    for (item in navItems) {
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon!!,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    // Aba Lançamentos sempre abre sem filtro (o filtro vem do
                                    // toque em conta/categoria e se limpa no chip "Limpar").
                                    if (item.route == "transactions") transactionsViewModel.clearFilters()
                                    if (item.route == navController.graph.startDestinationRoute) {
                                        navController.popBackStack(
                                            navController.graph.startDestinationId,
                                            inclusive = false,
                                        )
                                    } else {
                                        navController.navigate(item.route) {
                                            launchSingleTop = true
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            restoreState = true
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute in bottomRoutes) {
                    FloatingActionButton(
                        onClick = { showFastEntry = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Novo lançamento")
                    }
                }
            },
        ) { innerPadding ->
            MainNavGraph(
                navController = navController,
                navigateSingleTop = ::navigateSingleTop,
                transactionsViewModel = transactionsViewModel,
                fastEntryAccounts = fastEntryAccounts,
                fastEntryCategories = fastEntryCategories,
                showFastEntry = showFastEntry,
                onShowFastEntryChange = { showFastEntry = it },
                innerPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun MainNavGraph(
    navController: NavHostController,
    navigateSingleTop: (String) -> Unit,
    transactionsViewModel: TransactionsViewModel,
    fastEntryAccounts: List<Account>,
    fastEntryCategories: List<Category>,
    showFastEntry: Boolean,
    onShowFastEntryChange: (Boolean) -> Unit,
    innerPadding: PaddingValues,
) {
    val context = LocalContext.current
    val financeRepository = (context.applicationContext as LevApplication).container.financeRepository

    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = Modifier.padding(innerPadding),
    ) {
        composable("dashboard") {
            val dashboardViewModel: DashboardViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            DashboardScreen(
                viewModel = dashboardViewModel,
                onCardClick = { account ->
                    if (account.type == "CREDIT_CARD") {
                        navController.navigate("invoice_details/${account.id}")
                    }
                },
                onEmptyAction = { navigateSingleTop("accounts") },
                onOpenAccounts = { navigateSingleTop("accounts") },
                onAccountClick = { account ->
                    transactionsViewModel.setAccountFilter(account.id)
                    navController.navigate("transactions")
                },
                onBalanceClick = { navigateSingleTop("reports") },
            )
        }
        composable("transactions") {
            val transactions by transactionsViewModel.transactions.collectAsStateWithLifecycle()
            val accounts by transactionsViewModel.accounts.collectAsStateWithLifecycle()
            val categories by transactionsViewModel.categories.collectAsStateWithLifecycle()
            val accountFilter by transactionsViewModel.accountFilter.collectAsStateWithLifecycle()
            val categoryFilter by transactionsViewModel.categoryFilter.collectAsStateWithLifecycle()
            TransactionListScreen(
                transactions = transactions,
                accounts = accounts,
                categories = categories,
                onNewTransaction = { onShowFastEntryChange(true) },
                onTransactionUpdate = transactionsViewModel::updateTransaction,
                onTransactionDelete = transactionsViewModel::deleteTransaction,
                filterAccountId = accountFilter,
                filterCategoryId = categoryFilter,
                onClearFilters = transactionsViewModel::clearFilters,
            )
        }
        composable("accounts") {
            val accountsViewModel: AccountsViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            AccountsScreen(
                viewModel = accountsViewModel,
                onBack = { navController.popBackStack() },
                onAccountClick = { account ->
                    transactionsViewModel.setAccountFilter(account.id)
                    navController.navigate("transactions")
                },
            )
        }
        composable("cards") {
            CardsScreen(
                financeRepository = financeRepository,
                onBack = { navController.popBackStack() },
                onOpenInvoice = { id -> navController.navigate("invoice_details/$id") },
                onCreateCard = { navigateSingleTop("accounts") },
            )
        }
        composable("invoice_details/{accountId}") { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId") ?: return@composable
            InvoiceDetailsScreen(
                financeRepository = financeRepository,
                accountId = accountId,
                onBack = { navController.popBackStack() },
            )
        }
        composable("categories") {
            val categoriesViewModel: CategoriesViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            CategoriesScreen(
                viewModel = categoriesViewModel,
                onBack = { navController.popBackStack() },
                onCategoryClick = { category ->
                    transactionsViewModel.setCategoryFilter(category.id)
                    navController.navigate("transactions")
                },
            )
        }
        composable("planning") {
            val budgetViewModel: BudgetViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            val goalsViewModel: GoalsViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            val recurrencesViewModel: RecurrencesViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            PlanningScreen(
                budgetViewModel = budgetViewModel,
                goalsViewModel = goalsViewModel,
                recurrencesViewModel = recurrencesViewModel,
            )
        }
        composable("reports") {
            val reportsViewModel: ReportsViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            ReportsScreen(
                viewModel = reportsViewModel,
                onOpenPlanning = { navigateSingleTop("planning") },
                onOpenNetWorth = { navController.navigate("networth") },
                onOpenInsightTarget = { source ->
                    when (source) {
                        "dashboard" -> navigateSingleTop("dashboard")
                        "budget" -> navigateSingleTop("planning")
                        else -> navigateSingleTop("transactions")
                    }
                },
            )
        }
        composable("networth") {
            NetWorthScreen(onBack = { navController.popBackStack() })
        }
        composable("archive") {
            ArchiveScreen(onBack = { navController.popBackStack() })
        }
        composable("trash") {
            com.example.lev.ui.screens.trash.TrashScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable("importexport") {
            ImportExportScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            val settingsViewModel: SettingsViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            SettingsScreen(
                viewModel = settingsViewModel,
                onOpenCategories = { navController.navigate("categories") },
                onOpenSheets = { navController.navigate("sync") },
                onBack = { navController.popBackStack() },
            )
        }
        composable("sync") {
            val syncViewModel: SyncViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            SyncSettingsScreen(
                viewModel = syncViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }

    if (showFastEntry) {
        FastEntryBottomSheet(
            accounts = fastEntryAccounts,
            categories = fastEntryCategories,
            onDismiss = { onShowFastEntryChange(false) },
            onSave = transactionsViewModel::saveFastEntry,
        )
    }
}

/** Cabeçalho de telas internas com voltar — usado onde o TopAppBar é local. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = { Text(title, fontFamily = PoiretOne) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
        },
    )
}
