package com.example.ui.mealplanner

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MealPlanItem
import com.example.data.model.Recipe
import com.example.data.repository.MealPlanRepository
import com.example.data.repository.RecipeRepository
import com.example.data.repository.ShoppingRepository
import com.example.ui.components.EmptyStateView
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerScreen(
    mealPlanRepository: MealPlanRepository,
    recipeRepository: RecipeRepository,
    shoppingRepository: ShoppingRepository,
    onRecipeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val today = remember { java.time.LocalDate.now() }
    val currentMonday = remember { today.with(java.time.DayOfWeek.MONDAY) }
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var selectedDayIndex by remember { mutableIntStateOf(today.dayOfWeek.value - 1) }
    val selectedDate = remember(selectedDayIndex) { currentMonday.plusDays(selectedDayIndex.toLong()) }
    val selectedDateString = selectedDate.toString()

    val allPlans by mealPlanRepository.allMealPlans.collectAsState(initial = emptyList())
    var availableRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }

    var showAddMealDialog by remember { mutableStateOf(false) }
    var selectedMealType by remember { mutableStateOf("Lunch") }

    LaunchedEffect(Unit) {
        availableRecipes = recipeRepository.searchRecipes()
        // Seed default meal plan items if empty
        if (allPlans.isEmpty()) {
            val rec1 = availableRecipes.getOrNull(0)
            val rec2 = availableRecipes.getOrNull(1)
            val seedDate = LocalDate.now().toString()
            if (rec1 != null) {
                mealPlanRepository.addRecipeToMealPlan(seedDate, "Lunch", rec1, servesWho = "Noah")
            }
            if (rec2 != null) {
                mealPlanRepository.addRecipeToMealPlan(seedDate, "Dinner", rec2, servesWho = "Family (4)")
            }
        }
    }

    val dayPlans = allPlans.filter { it.dateString == selectedDateString }
    val totalDailyCalories = dayPlans.sumOf { it.calories }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Weekly Meal Planner", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (availableRecipes.isNotEmpty()) {
                                    for (i in 0..6) {
                                        val dateStr = currentMonday.plusDays(i.toLong()).toString()
                                        val bRec = availableRecipes[(i * 3) % availableRecipes.size]
                                        val lRec = availableRecipes[(i * 3 + 1) % availableRecipes.size]
                                        val dRec = availableRecipes[(i * 3 + 2) % availableRecipes.size]
                                        mealPlanRepository.addRecipeToMealPlan(dateStr, "Breakfast", bRec, servesWho = "Noah")
                                        mealPlanRepository.addRecipeToMealPlan(dateStr, "Lunch", lRec, servesWho = "Partner & Noah")
                                        mealPlanRepository.addRecipeToMealPlan(dateStr, "Dinner", dRec, servesWho = "Family (4)")
                                    }
                                    snackbarHostState.showSnackbar("AI Chef auto-planned your entire 7-day week! 🍽️")
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Auto-Plan Week", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = {
                            scope.launch {
                                dayPlans.forEach { plan ->
                                    recipeRepository.getRecipeById(plan.recipeId)?.let { recipe ->
                                        shoppingRepository.addRecipeIngredients(recipe)
                                    }
                                }
                                snackbarHostState.showSnackbar("Exported planned meal ingredients to Shopping List! 🛒")
                            }
                        }
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = "Generate Shopping List")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Days Selector Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(daysOfWeek.size) { index ->
                    val dayName = daysOfWeek[index]
                    val dateNum = currentMonday.plusDays(index.toLong()).dayOfMonth
                    val isSelected = selectedDayIndex == index

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedDayIndex = index },
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$dateNum",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Calorie & Nutrition Progress Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Calorie Target",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$totalDailyCalories / 2,000 kcal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    CircularProgressIndicator(
                        progress = { (totalDailyCalories.toFloat() / 2000f).coerceIn(0f, 1f) },
                        modifier = Modifier.size(42.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 5.dp
                    )
                }
            }

            // Meal Types (Breakfast, Lunch, Dinner, Snack)
            val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(mealTypes) { mealType ->
                    val mealItem = dayPlans.find { it.mealType.equals(mealType, ignoreCase = true) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (mealType) {
                                            "Breakfast" -> Icons.Default.WbSunny
                                            "Lunch" -> Icons.Default.Restaurant
                                            "Dinner" -> Icons.Default.NightsStay
                                            else -> Icons.Default.LocalCafe
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(mealType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }

                                if (mealItem == null) {
                                    IconButton(
                                        onClick = {
                                            selectedMealType = mealType
                                            showAddMealDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Meal")
                                    }
                                }
                            }

                            if (mealItem != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onRecipeClick(mealItem.recipeId) },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = mealItem.recipeTitle,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = "${mealItem.calories} kcal • ${mealItem.prepTimeMinutes}m prep",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = "👤 ${mealItem.servesWho}",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    mealPlanRepository.removeMealPlan(mealItem.id)
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No meal planned for $mealType yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add Meal Dialog
        if (showAddMealDialog) {
            var selectedServesWho by remember { mutableStateOf("Family") }
            val servesOptions = listOf("Family", "Noah", "Kids", "Guests", "Partner", "Self")

            AlertDialog(
                onDismissRequest = { showAddMealDialog = false },
                title = { Text("Add Recipe to $selectedMealType") },
                text = {
                    Column {
                        Text("Who is this meal for?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(servesOptions) { option ->
                                FilterChip(
                                    selected = selectedServesWho == option,
                                    onClick = { selectedServesWho = option },
                                    label = { Text(option) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableRecipes) { recipe ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            scope.launch {
                                                mealPlanRepository.addRecipeToMealPlan(
                                                    selectedDateString,
                                                    selectedMealType,
                                                    recipe,
                                                    servesWho = selectedServesWho
                                                )
                                                showAddMealDialog = false
                                            }
                                        },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(recipe.title, fontWeight = FontWeight.Bold)
                                            Text("${recipe.calories} kcal • ${recipe.totalTimeMinutes} mins", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Icon(Icons.Default.Add, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAddMealDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
