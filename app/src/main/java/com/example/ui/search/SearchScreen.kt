package com.example.ui.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Recipe
import com.example.data.repository.RecipeRepository
import com.example.ui.components.CategoryFilterChip
import com.example.ui.components.EmptyStateView
import com.example.ui.components.RecipeCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    recipeRepository: RecipeRepository,
    onRecipeClick: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCuisine by remember { mutableStateOf("All") }
    var selectedDifficulty by remember { mutableStateOf("All") }
    var selectedDietary by remember { mutableStateOf("All") }
    var showFilterSheet by remember { mutableStateOf(false) }

    var searchResults by remember { mutableStateOf<List<Recipe>>(emptyList()) }

    val cuisines = listOf("All", "Italian", "Asian", "Mexican", "Mediterranean", "American")
    val difficulties = listOf("All", "Easy", "Medium", "Hard")
    val dietaryOptions = listOf("All", "Vegetarian", "Vegan", "Gluten Free", "High Protein", "Low Carb", "Keto", "Air Fryer")

    fun triggerSearch() {
        scope.launch {
            searchResults = recipeRepository.searchRecipes(
                query = searchQuery,
                cuisine = if (selectedCuisine == "All") null else selectedCuisine,
                difficulty = if (selectedDifficulty == "All") null else selectedDifficulty,
                dietary = if (selectedDietary == "All") null else selectedDietary
            )
        }
    }

    LaunchedEffect(searchQuery, selectedCuisine, selectedDifficulty, selectedDietary) {
        triggerSearch()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by recipe name or ingredient...") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (selectedCuisine != "All" || selectedDifficulty != "All" || selectedDietary != "All") {
                                    Badge()
                                }
                            }
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
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
            // Quick Dietary Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dietaryOptions) { diet ->
                    CategoryFilterChip(
                        text = diet,
                        isSelected = selectedDietary == diet,
                        onClick = { selectedDietary = diet }
                    )
                }
            }

            // Results count header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${searchResults.size} Recipes Found",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                if (selectedCuisine != "All" || selectedDifficulty != "All" || selectedDietary != "All") {
                    TextButton(onClick = {
                        selectedCuisine = "All"
                        selectedDifficulty = "All"
                        selectedDietary = "All"
                        searchQuery = ""
                    }) {
                        Text("Reset Filters", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (searchResults.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.SearchOff,
                    title = "No Recipes Found",
                    description = "Try searching for a different ingredient, recipe name, or resetting your filter preferences.",
                    actionText = "Clear Search",
                    onActionClick = {
                        searchQuery = ""
                        selectedCuisine = "All"
                        selectedDifficulty = "All"
                        selectedDietary = "All"
                    }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(searchResults, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = { onRecipeClick(recipe.id) },
                            onFavoriteToggle = {
                                scope.launch {
                                    recipeRepository.toggleFavorite(recipe)
                                    triggerSearch()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Filter Bottom Sheet
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Filter Recipes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Cuisine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(cuisines) { cuisine ->
                            CategoryFilterChip(
                                text = cuisine,
                                isSelected = selectedCuisine == cuisine,
                                onClick = { selectedCuisine = cuisine }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Difficulty Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(difficulties) { diff ->
                            CategoryFilterChip(
                                text = diff,
                                isSelected = selectedDifficulty == diff,
                                onClick = { selectedDifficulty = diff }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply Filters (${searchResults.size} Results)")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
