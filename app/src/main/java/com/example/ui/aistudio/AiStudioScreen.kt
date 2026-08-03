package com.example.ui.aistudio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Recipe
import com.example.data.repository.RecipeRepository
import com.example.ui.components.CategoryFilterChip
import com.example.ui.components.RecipeCard
import kotlinx.coroutines.launch

import com.example.data.repository.PantryRepository
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudioScreen(
    recipeRepository: RecipeRepository,
    pantryRepository: PantryRepository? = null,
    onRecipeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Pantry Chef", "Camera Scanner", "Chef Q&A")

    // Pantry Chef State
    val pantryItems by (pantryRepository?.pantryItems ?: MutableStateFlow(emptyList())).collectAsState(initial = emptyList())
    var ingredientInput by remember { mutableStateOf("") }
    val selectedIngredients = remember { mutableStateListOf("Chicken Breast", "Garlic", "Spinach", "Heavy Cream") }
    var selectedDietary by remember { mutableStateOf("All") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedRecipe by remember { mutableStateOf<Recipe?>(null) }

    // Camera Scanner State
    var scannedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val detectedIngredients = remember { mutableStateListOf<String>() }
    var isScanning by remember { mutableStateOf(false) }

    // Chef Q&A State
    var qaQuestion by remember { mutableStateOf("") }
    var qaAnswer by remember { mutableStateOf<String?>(null) }
    var isQaLoading by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            scannedBitmap = bitmap
            bitmap?.let { b ->
                isScanning = true
                scope.launch {
                    val ingredients = recipeRepository.scanIngredientsFromImage(b)
                    detectedIngredients.clear()
                    detectedIngredients.addAll(ingredients)
                    isScanning = false
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RecipeAI Studio", fontWeight = FontWeight.Bold)
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
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
                    .padding(bottom = 80.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // Pantry Chef Tab
                        Column {
                            Text(
                                text = "What's in your fridge & pantry?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Select or type ingredients you have, and AI Chef will craft a custom recipe.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Input Box
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = ingredientInput,
                                    onValueChange = { ingredientInput = it },
                                    placeholder = { Text("Add ingredient (e.g. Avocado)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        if (ingredientInput.isNotBlank()) {
                                            selectedIngredients.add(ingredientInput.trim())
                                            ingredientInput = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (pantryItems.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        val stockNames = pantryItems.map { it.name }
                                        stockNames.forEach { name ->
                                            if (!selectedIngredients.contains(name)) {
                                                selectedIngredients.add(name)
                                            }
                                        }
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Loaded ${pantryItems.size} ingredient(s) from your Pantry Inventory! 📦")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Kitchen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Load from My Pantry Stock (${pantryItems.size} items) 📦")
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Ingredient Chips
                            Text("Your Selected Ingredients:", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRowLayout(
                                spacing = 8.dp
                            ) {
                                selectedIngredients.forEach { ing ->
                                    InputChip(
                                        selected = true,
                                        onClick = { selectedIngredients.remove(ing) },
                                        label = { Text(ing) },
                                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Dietary restriction optional
                            Text("Dietary Preference (Optional):", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("All", "High Protein", "Vegetarian", "Vegan", "Gluten Free", "Keto")) { diet ->
                                    CategoryFilterChip(
                                        text = diet,
                                        isSelected = selectedDietary == diet,
                                        onClick = { selectedDietary = diet }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (selectedIngredients.isNotEmpty()) {
                                        isGenerating = true
                                        scope.launch {
                                            val recipe = recipeRepository.generateRecipeFromPantry(
                                                ingredients = selectedIngredients,
                                                dietary = if (selectedDietary == "All") null else selectedDietary
                                            )
                                            generatedRecipe = recipe
                                            isGenerating = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = !isGenerating && selectedIngredients.isNotEmpty()
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Generating Custom AI Recipe...")
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate Recipe from Pantry ✨", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (generatedRecipe != null) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("Generated AI Recipe Result:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                RecipeCard(
                                    recipe = generatedRecipe!!,
                                    onClick = { onRecipeClick(generatedRecipe!!.id) },
                                    onFavoriteToggle = {
                                        scope.launch {
                                            recipeRepository.toggleFavorite(generatedRecipe!!)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    1 -> {
                        // Camera Scanner Tab
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Scan Kitchen Ingredients",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "Upload a photo of your fridge, pantry, or food items. AI Vision will recognize ingredients automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .clickable { photoPickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (scannedBitmap != null) {
                                    Image(
                                        bitmap = scannedBitmap!!.asImageBitmap(),
                                        contentDescription = "Scanned photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Upload",
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Tap to Select Photo from Gallery", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (isScanning) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("AI Vision is analyzing ingredients in photo...")
                                }
                            } else if (detectedIngredients.isNotEmpty()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Detected Ingredients (${detectedIngredients.size}):", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    FlowRowLayout(spacing = 8.dp) {
                                        detectedIngredients.forEach { ing ->
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(ing) },
                                                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = {
                                            selectedIngredients.clear()
                                            selectedIngredients.addAll(detectedIngredients)
                                            selectedTabIndex = 0
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Cook with Detected Ingredients 🎉")
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Chef Q&A Tab
                        Column {
                            Text(
                                text = "Ask Chef AI Assistant",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Get professional cooking advice, technique explanations, or ingredient substitution tips.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = qaQuestion,
                                onValueChange = { qaQuestion = it },
                                placeholder = { Text("Ask anything e.g. What can I substitute for buttermilk?") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                shape = RoundedCornerShape(16.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (qaQuestion.isNotBlank()) {
                                        isQaLoading = true
                                        scope.launch {
                                            qaAnswer = recipeRepository.askChef(qaQuestion)
                                            isQaLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isQaLoading && qaQuestion.isNotBlank()
                            ) {
                                if (isQaLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("Ask Chef AI")
                                }
                            }

                            if (qaAnswer != null) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Chef AI Response:", fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(qaAnswer!!, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRowLayout(
    spacing: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        content()
    }
}
