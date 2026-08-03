package com.example.ui.recipe

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Recipe
import com.example.data.repository.RecipeRepository
import com.example.data.repository.ShoppingRepository
import com.example.ui.components.CategoryFilterChip
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    recipeRepository: RecipeRepository,
    shoppingRepository: ShoppingRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentRecipe by remember { mutableStateOf<Recipe?>(null) }
    var currentServings by remember { mutableStateOf(4) }
    var isFavorite by remember { mutableStateOf(false) }
    var showAiConvertSheet by remember { mutableStateOf(false) }
    var showAskChefDialog by remember { mutableStateOf(false) }

    var chefQuestion by remember { mutableStateOf("") }
    var chefAnswer by remember { mutableStateOf<String?>(null) }
    var isChefLoading by remember { mutableStateOf(false) }
    var isAiConverting by remember { mutableStateOf(false) }

    val completedSteps = remember { mutableStateListOf<Int>() }
    val checkedIngredients = remember { mutableStateListOf<Int>() }

    var activeTimerSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var activeTimerStepLabel by remember { mutableStateOf("") }

    var speakingStepNumber by remember { mutableStateOf<Int?>(null) }
    var isSpeakingAudio by remember { mutableStateOf(false) }

    // Active Timer Effect
    LaunchedEffect(isTimerRunning, activeTimerSeconds) {
        if (isTimerRunning && activeTimerSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            activeTimerSeconds -= 1
            if (activeTimerSeconds == 0) {
                isTimerRunning = false
                snackbarHostState.showSnackbar("🔔 RING RING! $activeTimerStepLabel timer completed! ⏱️")
            }
        }
    }

    // Hands-Free Audio Voice Assistant Effect
    LaunchedEffect(isSpeakingAudio, speakingStepNumber) {
        if (isSpeakingAudio && speakingStepNumber != null) {
            kotlinx.coroutines.delay(5000L) // Simulates voice reading step
            val totalSteps = currentRecipe?.instructions?.size ?: 0
            if ((speakingStepNumber ?: 0) < totalSteps) {
                speakingStepNumber = (speakingStepNumber ?: 0) + 1
            } else {
                isSpeakingAudio = false
                speakingStepNumber = null
                snackbarHostState.showSnackbar("Finished reading all cooking steps! 👩‍🍳")
            }
        }
    }

    LaunchedEffect(recipeId) {
        val loaded = recipeRepository.getRecipeById(recipeId)
        if (loaded != null) {
            currentRecipe = loaded
            currentServings = loaded.servings
            isFavorite = recipeRepository.isFavorite(loaded.id)
        }
    }

    val recipe = currentRecipe

    if (recipe == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val imageRes = when (recipe.imageUrl) {
        "img_recipe_hero_1785372521529" -> R.drawable.img_recipe_hero_1785372521529
        "img_pantry_ai_1785372538440" -> R.drawable.img_pantry_ai_1785372538440
        else -> R.drawable.img_recipe_hero_1785372521529
    }

    val servingMultiplier = currentServings.toDouble() / recipe.servings.coerceAtLeast(1)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAskChefDialog = true },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Ask Chef AI") },
                text = { Text("Ask Chef AI") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            // Hero Image Header with floating top buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = recipe.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )

                // Back and Action top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Check out this recipe: ${recipe.title} on RecipeAI!")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Recipe"))
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }

                        IconButton(
                            onClick = {
                                scope.launch {
                                    recipeRepository.toggleFavorite(recipe)
                                    val newFav = !isFavorite
                                    isFavorite = newFav
                                    currentRecipe = currentRecipe?.copy(isFavorite = newFav)
                                    snackbarHostState.showSnackbar(
                                        if (newFav) "Saved to Favourites!" else "Removed from Favourites"
                                    )
                                }
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color.Red else Color.White
                            )
                        }
                    }
                }

                // Title & Category in Hero Overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = recipe.category.uppercase(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "By ${recipe.author} • ${recipe.cuisine}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // Overview Chips: Rating, Prep Time, Cook Time, Total Time
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${recipe.rating}", fontWeight = FontWeight.Bold)
                        }
                        Text("${recipe.reviewCount} reviews", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Divider(modifier = Modifier.height(32.dp).width(1.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${recipe.prepTimeMinutes}m", fontWeight = FontWeight.Bold)
                        }
                        Text("Prep Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Divider(modifier = Modifier.height(32.dp).width(1.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${recipe.cookTimeMinutes}m", fontWeight = FontWeight.Bold)
                        }
                        Text("Cook Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Description
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                // AI Dietary Converter Shortcut Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showAiConvertSheet = true },
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "AI Dietary Converter",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Convert to Vegan, Keto, Gluten-Free, etc.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Servings Adjuster
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Servings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = { if (currentServings > 1) currentServings-- },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }

                            Text(
                                text = "$currentServings portions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            IconButton(
                                onClick = { currentServings++ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nutrition Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Nutritional Value (per serving)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MacroMetric("Calories", "${(recipe.calories * (currentServings.toDouble() / recipe.servings)).toInt()} kcal")
                            MacroMetric("Protein", "${(recipe.proteinGrams * servingMultiplier).toInt()}g")
                            MacroMetric("Carbs", "${(recipe.carbsGrams * servingMultiplier).toInt()}g")
                            MacroMetric("Fats", "${(recipe.fatGrams * servingMultiplier).toInt()}g")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Ingredients Checklist
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ingredients (${recipe.ingredients.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = {
                        scope.launch {
                            shoppingRepository.addRecipeIngredients(recipe)
                            snackbarHostState.showSnackbar("Added ingredients to Shopping List! 🛒")
                        }
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add to List")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                recipe.ingredients.forEachIndexed { index, ing ->
                    val isChecked = checkedIngredients.contains(index)
                    val scaledAmount = String.format(java.util.Locale.US, "%.1f", ing.amount * servingMultiplier).removeSuffix(".0")

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (isChecked) checkedIngredients.remove(index) else checkedIngredients.add(index)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it == true) checkedIngredients.add(index) else checkedIngredients.remove(index)
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = ing.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "$scaledAmount ${ing.unit}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cooking Instructions Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Step-by-Step Instructions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    AssistChip(
                        onClick = {
                            if (isSpeakingAudio) {
                                isSpeakingAudio = false
                                speakingStepNumber = null
                            } else {
                                isSpeakingAudio = true
                                speakingStepNumber = 1
                            }
                        },
                        label = { Text(if (isSpeakingAudio) "Pause Voice" else "🔊 Read Aloud") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSpeakingAudio) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active Step Timer Card
                if (activeTimerSeconds > 0 || isTimerRunning) {
                    val minutes = activeTimerSeconds / 60
                    val seconds = activeTimerSeconds % 60
                    val timeFormatted = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "$activeTimerStepLabel Timer: $timeFormatted",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = if (isTimerRunning) "Counting down..." else "Paused",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = { isTimerRunning = !isTimerRunning }) {
                                    Icon(if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                                }
                                IconButton(onClick = {
                                    isTimerRunning = false
                                    activeTimerSeconds = 0
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel Timer")
                                }
                            }
                        }
                    }
                }

                // Active Audio Voice Assistant Banner
                if (isSpeakingAudio && speakingStepNumber != null) {
                    val currentStepObj = recipe.instructions.find { it.stepNumber == speakingStepNumber }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🔊 Voice Guide - Step $speakingStepNumber of ${recipe.instructions.size}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentStepObj?.description ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                recipe.instructions.forEach { step ->
                    val isDone = completedSteps.contains(step.stepNumber)
                    val isBeingSpoken = speakingStepNumber == step.stepNumber

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                if (isDone) completedSteps.remove(step.stepNumber) else completedSteps.add(step.stepNumber)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isBeingSpoken) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            else if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    } else {
                                        Text(
                                            text = "${step.stepNumber}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = "Step ${step.stepNumber}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                step.timerMinutes?.let { mins ->
                                    Spacer(modifier = Modifier.weight(1f))
                                    AssistChip(
                                        onClick = {
                                            activeTimerStepLabel = "Step ${step.stepNumber}"
                                            activeTimerSeconds = mins * 60
                                            isTimerRunning = true
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Started $mins minute countdown for Step ${step.stepNumber}! ⏱️")
                                            }
                                        },
                                        label = { Text("${mins}m Timer") },
                                        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = step.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (recipe.tips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Chef Tips 💡",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    recipe.tips.forEach { tip ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "• $tip",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Ask Chef AI Dialog
        if (showAskChefDialog) {
            AlertDialog(
                onDismissRequest = { showAskChefDialog = false },
                title = { Text("Ask Chef AI 👨‍🍳", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Ask any cooking question or ask for substitute ingredients for ${recipe.title}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = chefQuestion,
                            onValueChange = { chefQuestion = it },
                            placeholder = { Text("e.g. Can I use olive oil instead of butter?") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isChefLoading) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else if (chefAnswer != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = chefAnswer!!,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (chefQuestion.isNotBlank()) {
                                isChefLoading = true
                                scope.launch {
                                    chefAnswer = recipeRepository.askChef(chefQuestion, recipe.title)
                                    isChefLoading = false
                                }
                            }
                        }
                    ) {
                        Text("Ask")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAskChefDialog = false
                        chefAnswer = null
                        chefQuestion = ""
                    }) {
                        Text("Close")
                    }
                }
            )
        }

        // AI Dietary Converter Sheet
        if (showAiConvertSheet) {
            ModalBottomSheet(onDismissRequest = { showAiConvertSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Convert Recipe with AI ✨",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Choose a dietary style. Chef AI will replace ingredients while keeping flavor intact.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isAiConverting) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Converting recipe with Chef AI...")
                            }
                        }
                    } else {
                        val diets = listOf("Vegetarian", "Vegan", "Gluten Free", "Keto", "High Protein", "Low Carb")
                        diets.forEach { diet ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        isAiConverting = true
                                        scope.launch {
                                            val converted = recipeRepository.convertRecipeDietary(recipe, diet)
                                            currentRecipe = converted
                                            isAiConverting = false
                                            showAiConvertSheet = false
                                            snackbarHostState.showSnackbar("Converted recipe to $diet style! 🎉")
                                        }
                                    },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(diet, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MacroMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
