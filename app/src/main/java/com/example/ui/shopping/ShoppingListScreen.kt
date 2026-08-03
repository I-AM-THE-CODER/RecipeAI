package com.example.ui.shopping

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ShoppingItem
import com.example.data.repository.ShoppingRepository
import com.example.ui.components.EmptyStateView
import kotlinx.coroutines.launch

import com.example.data.model.PantryItem
import com.example.data.repository.PantryRepository
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    shoppingRepository: ShoppingRepository,
    pantryRepository: PantryRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val items by shoppingRepository.shoppingItems.collectAsState(initial = emptyList())
    val pantryItems by (pantryRepository?.pantryItems ?: MutableStateFlow(emptyList())).collectAsState(initial = emptyList())

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Shopping, 1: Pantry
    var showAddItemDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemAmount by remember { mutableStateOf("1") }
    var newItemUnit by remember { mutableStateOf("item") }
    var newItemCategory by remember { mutableStateOf("Pantry") }

    val categories = listOf("Produce", "Dairy & Eggs", "Meat & Seafood", "Pantry", "Spices & Oils")

    // Seed default items if completely empty
    LaunchedEffect(Unit) {
        if (items.isEmpty()) {
            shoppingRepository.addItem("Heavy Cream", 1.0, "cup", "Dairy & Eggs", "Tuscan Garlic Chicken")
            shoppingRepository.addItem("Baby Spinach", 2.0, "cups", "Produce", "Tuscan Garlic Chicken")
            shoppingRepository.addItem("Sun-dried Tomatoes", 0.5, "cup", "Pantry", "Tuscan Garlic Chicken")
            shoppingRepository.addItem("Garlic Cloves", 4.0, "cloves", "Produce", "Tuscan Garlic Chicken")
            shoppingRepository.addItem("Extra Firm Tofu", 1.0, "block", "Produce", "Air Fryer Tofu")
        }
        pantryRepository?.seedInitialPantryIfEmpty(pantryItems)
    }

    val completedCount = items.count { it.isCompleted }
    val totalCount = items.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (selectedTabIndex == 0) "Shopping List" else "Pantry Inventory", fontWeight = FontWeight.Bold) },
                actions = {
                    if (selectedTabIndex == 0) {
                        if (completedCount > 0 && pantryRepository != null) {
                            IconButton(onClick = {
                                scope.launch {
                                    shoppingRepository.transferCompletedToPantry(pantryRepository, items)
                                    snackbarHostState.showSnackbar("Moved $completedCount checked item(s) to your Pantry Inventory! 📦")
                                }
                            }) {
                                Icon(Icons.Default.MoveToInbox, contentDescription = "Move to Pantry", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        if (items.isNotEmpty()) {
                            IconButton(onClick = {
                                val listText = items.groupBy { it.category }.entries.joinToString("\n\n") { (cat, list) ->
                                    "🛒 $cat:\n" + list.joinToString("\n") { " [${if (it.isCompleted) "x" else " "}] ${it.amount} ${it.unit} ${it.ingredientName}" }
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "RecipeAI Shopping List:\n\n$listText")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Shopping List"))
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share List")
                            }

                            IconButton(onClick = {
                                scope.launch { shoppingRepository.clearCompleted() }
                            }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Completed")
                            }
                        }
                    } else {
                        IconButton(onClick = {
                            scope.launch {
                                pantryRepository?.clearPantry()
                                snackbarHostState.showSnackbar("Cleared pantry inventory")
                            }
                        }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Clear Pantry")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (pantryRepository != null) {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Shopping List (${items.size})", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Pantry Stock (${pantryItems.size})", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Kitchen, contentDescription = null) }
                    )
                }
            }

            if (selectedTabIndex == 0) {
                // Shopping List View
                if (totalCount > 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Aisle Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("$completedCount of $totalCount items", style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }

                if (items.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.ShoppingCart,
                        title = "Shopping List is Empty",
                        description = "Add ingredients from any recipe, meal plan, or create custom items using the '+' button.",
                        actionText = "Add Custom Item",
                        onActionClick = { showAddItemDialog = true }
                    )
                } else {
                    val grouped = items.groupBy { it.category }

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        grouped.forEach { (category, categoryItems) ->
                            item {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            items(categoryItems, key = { it.id }) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            scope.launch {
                                                shoppingRepository.toggleItem(item.id, !item.isCompleted)
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (item.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = item.isCompleted,
                                            onCheckedChange = {
                                                scope.launch {
                                                    shoppingRepository.toggleItem(item.id, it)
                                                }
                                            }
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.ingredientName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                            )

                                            if (item.recipeSourceTitle.orEmpty().isNotBlank()) {
                                                Text(
                                                    text = "From: ${item.recipeSourceTitle}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${item.amount} ${item.unit}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )

                                        IconButton(onClick = {
                                            scope.launch { shoppingRepository.deleteItem(item.id) }
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Pantry Stock View
                if (pantryItems.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Kitchen,
                        title = "Pantry Inventory is Empty",
                        description = "Keep track of ingredients in your fridge or pantry to find instant recipe ideas.",
                        actionText = "Add Pantry Stock",
                        onActionClick = { showAddItemDialog = true }
                    )
                } else {
                    val pantryGrouped = pantryItems.groupBy { it.category }

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 100.dp, top = 12.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        pantryGrouped.forEach { (category, stockItems) ->
                            item {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            items(stockItems, key = { it.id }) { pItem ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pItem.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${pItem.amount} ${pItem.unit}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(onClick = {
                                            scope.launch {
                                                pantryRepository?.deletePantryItem(pItem.id)
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Item Dialog
        if (showAddItemDialog) {
            AlertDialog(
                onDismissRequest = { showAddItemDialog = false },
                title = { Text("Add Custom Shopping Item") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newItemName,
                            onValueChange = { newItemName = it },
                            label = { Text("Ingredient Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newItemAmount,
                                onValueChange = { newItemAmount = it },
                                label = { Text("Amount") },
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = newItemUnit,
                                onValueChange = { newItemUnit = it },
                                label = { Text("Unit") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text("Category / Aisle:", style = MaterialTheme.typography.labelMedium)
                        LazyColumn(modifier = Modifier.height(120.dp)) {
                            items(categories) { cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { newItemCategory = cat }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = newItemCategory == cat, onClick = { newItemCategory = cat })
                                    Text(cat)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newItemName.isNotBlank()) {
                                scope.launch {
                                    shoppingRepository.addItem(
                                        name = newItemName.trim(),
                                        amount = newItemAmount.toDoubleOrNull() ?: 1.0,
                                        unit = newItemUnit.trim(),
                                        category = newItemCategory
                                    )
                                    newItemName = ""
                                    showAddItemDialog = false
                                }
                            }
                        }
                    ) {
                        Text("Add Item")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddItemDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
