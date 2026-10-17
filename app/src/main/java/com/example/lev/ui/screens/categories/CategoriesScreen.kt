package com.example.lev.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lev.data.model.Category
import com.example.lev.ui.components.EmptyState
import com.example.lev.ui.components.LevCard
import com.example.lev.ui.theme.PoiretOne

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onCategoryClick: (Category) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorias", fontFamily = PoiretOne) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar categoria")
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        if (uiState.categories.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = "Nenhuma categoria cadastrada",
                    description = "Toque no + para criar uma categoria.",
                )
            }
        } else {
            CategoryList(
                categories = uiState.categories,
                onCategoryClick = onCategoryClick,
                onDeleteClick = { viewModel.deleteCategory(it) },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (showCreateDialog) {
        CreateCategoryDialog(
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryList(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val roots = categories.filter { it.parentId == null }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // 92dp de folga: evita que o FAB fixo sobrepinja a última linha.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(roots, key = { it.id }) { category ->
            CategoryItem(
                category = category,
                onClick = { onCategoryClick(category) },
                onDelete = { onDeleteClick(category.id) },
            )
            val children = categories.filter { it.parentId == category.id }
            if (children.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    children.forEach { child ->
                        CategoryItem(
                            category = child,
                            onClick = { onCategoryClick(child) },
                            onDelete = { onDeleteClick(child.id) },
                            indent = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: Category,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    indent: Boolean = false,
) {
    val kindLabel = if (category.kind == "INCOME") "Receita" else "Despesa"
    val color =
        try {
            Color(android.graphics.Color.parseColor(category.color))
        } catch (_: IllegalArgumentException) {
            MaterialTheme.colorScheme.primary
        }
    LevCard(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = if (indent) 16.dp else 0.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = kindLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ponto de cor = identidade visual da categoria (hex do dado).
                // A letra do tipo era redundante com o rótulo à esquerda.
                Box(
                    modifier =
                        Modifier
                            .padding(end = 12.dp)
                            .size(10.dp)
                            .background(color = color, shape = CircleShape),
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir ${category.name}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCategoryDialog(
    viewModel: CategoriesViewModel,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedKind by rememberSaveable { mutableStateOf("EXPENSE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Categoria") },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedKind == "INCOME",
                            onClick = { selectedKind = "INCOME" },
                        )
                        Text("Receita")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedKind == "EXPENSE",
                            onClick = { selectedKind = "EXPENSE" },
                        )
                        Text("Despesa")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createCategory(name = name, kind = selectedKind)
                    }
                    onDismiss()
                },
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
