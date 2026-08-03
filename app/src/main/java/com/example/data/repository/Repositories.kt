package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.model.remote.GeminiAiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class RecipeRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val geminiService = GeminiAiService(context)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val recipeAdapter = moshi.adapter(Recipe::class.java)

    private val _predefinedRecipes = MutableStateFlow(getInitialSeedRecipes())
    val predefinedRecipes: StateFlow<List<Recipe>> = _predefinedRecipes

    val favoriteEntities: Flow<List<FavoriteRecipeEntity>> = database.favoritesDao().getAllFavorites()

    val favoriteRecipes: Flow<List<Recipe>> = favoriteEntities.map { entities ->
        entities.mapNotNull { entity ->
            try {
                recipeAdapter.fromJson(entity.jsonContent)?.copy(isFavorite = true)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getRecipeById(id: String): Recipe? = withContext(Dispatchers.IO) {
        // Check favorite DB first
        val fav = database.favoritesDao().getFavoriteById(id)
        if (fav != null) {
            try {
                return@withContext recipeAdapter.fromJson(fav.jsonContent)?.copy(isFavorite = true)
            } catch (e: Exception) {
                // fallback
            }
        }
        _predefinedRecipes.value.find { it.id == id }
    }

    suspend fun toggleFavorite(recipe: Recipe) = withContext(Dispatchers.IO) {
        val fav = database.favoritesDao().getFavoriteById(recipe.id)
        if (fav != null) {
            database.favoritesDao().deleteFavorite(recipe.id)
        } else {
            val updated = recipe.copy(isFavorite = true)
            val json = recipeAdapter.toJson(updated)
            database.favoritesDao().insertFavorite(FavoriteRecipeEntity(recipe.id, json))
        }
    }

    suspend fun isFavorite(recipeId: String): Boolean = withContext(Dispatchers.IO) {
        database.favoritesDao().getFavoriteById(recipeId) != null
    }

    suspend fun searchRecipes(
        query: String = "",
        category: String? = null,
        cuisine: String? = null,
        maxTimeMinutes: Int? = null,
        difficulty: String? = null,
        dietary: String? = null
    ): List<Recipe> = withContext(Dispatchers.IO) {
        _predefinedRecipes.value.filter { recipe ->
            val matchesQuery = query.isBlank() ||
                    recipe.title.contains(query, ignoreCase = true) ||
                    recipe.ingredients.any { it.name.contains(query, ignoreCase = true) } ||
                    recipe.description.contains(query, ignoreCase = true)

            val matchesCategory = category == null || category == "All" ||
                    recipe.category.equals(category, ignoreCase = true) ||
                    recipe.dietaryTags.any { it.equals(category, ignoreCase = true) }
            val matchesCuisine = cuisine == null || cuisine == "All" || recipe.cuisine.equals(cuisine, ignoreCase = true)
            val matchesTime = maxTimeMinutes == null || recipe.totalTimeMinutes <= maxTimeMinutes
            val matchesDifficulty = difficulty == null || difficulty == "All" || recipe.difficulty.equals(difficulty, ignoreCase = true)
            val matchesDietary = dietary == null || dietary == "All" || recipe.dietaryTags.any { it.contains(dietary, ignoreCase = true) }

            matchesQuery && matchesCategory && matchesCuisine && matchesTime && matchesDifficulty && matchesDietary
        }
    }

    suspend fun generateRecipeFromPantry(
        ingredients: List<String>,
        dietary: String? = null,
        mealType: String? = null
    ): Recipe {
        val newRecipe = geminiService.generateRecipeFromIngredients(ingredients, dietary, mealType)
        _predefinedRecipes.value = listOf(newRecipe) + _predefinedRecipes.value
        return newRecipe
    }

    suspend fun scanIngredientsFromImage(bitmap: android.graphics.Bitmap): List<String> {
        return geminiService.scanIngredientsFromImage(bitmap)
    }

    suspend fun convertRecipeDietary(recipe: Recipe, targetDiet: String): Recipe {
        val converted = geminiService.convertRecipeDietary(recipe, targetDiet)
        _predefinedRecipes.value = listOf(converted) + _predefinedRecipes.value
        return converted
    }

    suspend fun askChef(question: String, contextRecipeTitle: String? = null): String {
        return geminiService.askChefQuestion(question, contextRecipeTitle)
    }

    private fun getInitialSeedRecipes(): List<Recipe> {
        return listOf(
            Recipe(
                id = "rec_001",
                title = "Tuscan Garlic Butter Chicken",
                description = "Tender pan-seared chicken breasts smothered in a rich garlic, sun-dried tomato, and spinach cream sauce.",
                author = "Chef Isabella Rossi",
                rating = 4.9,
                reviewCount = 312,
                prepTimeMinutes = 15,
                cookTimeMinutes = 20,
                difficulty = "Medium",
                servings = 4,
                calories = 520,
                proteinGrams = 42,
                carbsGrams = 12,
                fatGrams = 34,
                category = "Dinner",
                cuisine = "Italian",
                imageUrl = "img_recipe_hero_1785372521529",
                dietaryTags = listOf("High Protein", "Low Carb", "Keto"),
                ingredients = listOf(
                    RecipeIngredient("Boneless Chicken Breasts", 4.0, "pieces", "Meat & Seafood"),
                    RecipeIngredient("Heavy Cream", 1.0, "cup", "Dairy & Eggs"),
                    RecipeIngredient("Sun-dried Tomatoes", 0.5, "cup", "Pantry"),
                    RecipeIngredient("Baby Spinach", 2.0, "cups", "Produce"),
                    RecipeIngredient("Garlic Cloves (minced)", 4.0, "cloves", "Produce"),
                    RecipeIngredient("Parmesan Cheese (grated)", 0.5, "cup", "Dairy & Eggs"),
                    RecipeIngredient("Olive Oil", 2.0, "tbsp", "Spices & Oils")
                ),
                instructions = listOf(
                    RecipeInstruction(1, "Season chicken breasts generously with salt, pepper, and Italian herbs.", null),
                    RecipeInstruction(2, "Heat olive oil in a large skillet over medium-high heat. Sear chicken 5-7 minutes per side until golden brown and cooked through.", 12),
                    RecipeInstruction(3, "Remove chicken from skillet and set aside on a warm plate.", null),
                    RecipeInstruction(4, "In the same skillet, sauté minced garlic and sun-dried tomatoes for 2 minutes until fragrant.", 2),
                    RecipeInstruction(5, "Pour in heavy cream and chicken broth. Bring to a gentle simmer, then stir in grated parmesan cheese.", 3),
                    RecipeInstruction(6, "Add spinach and let wilt. Return chicken breasts to sauce, spooning sauce over top, and serve warm.", 3)
                ),
                tips = listOf("Use sun-dried tomatoes packed in olive oil for maximum flavor.", "Garnish with fresh basil before serving."),
                equipment = listOf("Cast Iron Skillet", "Tongs", "Chef Knife")
            ),
            Recipe(
                id = "rec_002",
                title = "Air Fryer Crispy Sesame Tofu Bowl",
                description = "Ultra crispy golden air-fried tofu tossed in a sticky sweet chili sesame sauce over jasmine rice and avocado.",
                author = "Chef Lin Chen",
                rating = 4.8,
                reviewCount = 189,
                prepTimeMinutes = 10,
                cookTimeMinutes = 15,
                difficulty = "Easy",
                servings = 2,
                calories = 410,
                proteinGrams = 24,
                carbsGrams = 52,
                fatGrams = 14,
                category = "Lunch",
                cuisine = "Asian",
                imageUrl = "img_pantry_ai_1785372538440",
                dietaryTags = listOf("Vegetarian", "Vegan", "Gluten Free", "Air Fryer"),
                ingredients = listOf(
                    RecipeIngredient("Extra Firm Tofu (pressed)", 1.0, "block", "Produce"),
                    RecipeIngredient("Cornstarch", 2.0, "tbsp", "Pantry"),
                    RecipeIngredient("Low-Sodium Tamari", 2.0, "tbsp", "Pantry"),
                    RecipeIngredient("Sesame Oil", 1.0, "tbsp", "Spices & Oils"),
                    RecipeIngredient("Sweet Chili Sauce", 3.0, "tbsp", "Pantry"),
                    RecipeIngredient("Jasmine Rice (cooked)", 2.0, "cups", "Pantry"),
                    RecipeIngredient("Sliced Avocado", 1.0, "whole", "Produce"),
                    RecipeIngredient("Toasted Sesame Seeds", 1.0, "tbsp", "Spices & Oils")
                ),
                instructions = listOf(
                    RecipeInstruction(1, "Press tofu to remove excess moisture, then cut into 1-inch bite-sized cubes.", null),
                    RecipeInstruction(2, "Toss tofu cubes in tamari, sesame oil, and cornstarch until evenly coated.", null),
                    RecipeInstruction(3, "Preheat air fryer to 200°C (400°F). Arrange tofu in a single layer.", null),
                    RecipeInstruction(4, "Air fry for 15 minutes, shaking basket halfway through until crispy and golden.", 15),
                    RecipeInstruction(5, "Toss crispy tofu in sweet chili sauce. Serve over warm jasmine rice with avocado and sesame seeds.", null)
                ),
                tips = listOf("Pressing tofu for at least 15 minutes is key to ultimate crispiness."),
                equipment = listOf("Air Fryer", "Mixing Bowl", "Tofu Press")
            ),
            Recipe(
                id = "rec_003",
                title = "Chipotle Grilled Salmon Tacos",
                description = "Flaky chipotle-lime marinated salmon in warm corn tortillas topped with fresh mango salsa and avocado crema.",
                author = "Chef Mateo Silva",
                rating = 4.9,
                reviewCount = 245,
                prepTimeMinutes = 20,
                cookTimeMinutes = 10,
                difficulty = "Medium",
                servings = 3,
                calories = 460,
                proteinGrams = 34,
                carbsGrams = 38,
                fatGrams = 18,
                category = "Dinner",
                cuisine = "Mexican",
                imageUrl = "img_recipe_hero_1785372521529",
                dietaryTags = listOf("High Protein", "Gluten Free", "Healthy"),
                ingredients = listOf(
                    RecipeIngredient("Salmon Fillet (cubed)", 500.0, "g", "Meat & Seafood"),
                    RecipeIngredient("Chipotle Powder", 1.0, "tsp", "Spices & Oils"),
                    RecipeIngredient("Lime Juice", 3.0, "tbsp", "Produce"),
                    RecipeIngredient("Corn Tortillas", 6.0, "small", "Bakery"),
                    RecipeIngredient("Ripe Mango (diced)", 1.0, "cup", "Produce"),
                    RecipeIngredient("Red Onion (finely diced)", 0.25, "cup", "Produce"),
                    RecipeIngredient("Cilantro", 0.5, "bunch", "Produce"),
                    RecipeIngredient("Sour Cream / Greek Yogurt", 0.5, "cup", "Dairy & Eggs")
                ),
                instructions = listOf(
                    RecipeInstruction(1, "Marinate salmon cubes with chipotle powder, lime juice, salt, and olive oil for 10 minutes.", 10),
                    RecipeInstruction(2, "Prepare mango salsa: mix diced mango, red onion, cilantro, and a dash of lime juice.", null),
                    RecipeInstruction(3, "Sear salmon in a skillet or grill pan on high heat for 6-8 minutes until caramelized.", 8),
                    RecipeInstruction(4, "Warm corn tortillas on dry skillet.", null),
                    RecipeInstruction(5, "Assemble tacos with chipotle salmon, mango salsa, and dollop of avocado crema.", null)
                ),
                tips = listOf("Warm tortillas until pliable so they don't break when folded."),
                equipment = listOf("Grill Pan", "Mixing Bowl", "Knife")
            ),
            Recipe(
                id = "rec_004",
                title = "Greek Avocado & Feta Protein Salad",
                description = "Refreshing Mediterranean salad packed with cucumbers, cherry tomatoes, Kalamata olives, chickpeas, and creamy feta.",
                author = "Chef Elena Pappas",
                rating = 4.7,
                reviewCount = 142,
                prepTimeMinutes = 12,
                cookTimeMinutes = 0,
                difficulty = "Easy",
                servings = 2,
                calories = 360,
                proteinGrams = 18,
                carbsGrams = 30,
                fatGrams = 20,
                category = "Healthy",
                cuisine = "Mediterranean",
                imageUrl = "img_pantry_ai_1785372538440",
                dietaryTags = listOf("Vegetarian", "Gluten Free", "Healthy", "Quick Meals", "Budget Meals"),
                ingredients = listOf(
                    RecipeIngredient("English Cucumber (diced)", 1.0, "whole", "Produce"),
                    RecipeIngredient("Cherry Tomatoes (halved)", 1.5, "cups", "Produce"),
                    RecipeIngredient("Chickpeas (rinsed)", 1.0, "can", "Pantry"),
                    RecipeIngredient("Feta Cheese (crumbled)", 0.5, "cup", "Dairy & Eggs"),
                    RecipeIngredient("Kalamata Olives", 0.33, "cup", "Pantry"),
                    RecipeIngredient("Extra Virgin Olive Oil", 2.0, "tbsp", "Spices & Oils"),
                    RecipeIngredient("Dried Oregano", 1.0, "tsp", "Spices & Oils")
                ),
                instructions = listOf(
                    RecipeInstruction(1, "In a large salad bowl, combine cucumber, cherry tomatoes, rinsed chickpeas, and Kalamata olives.", null),
                    RecipeInstruction(2, "Whisk olive oil, lemon juice, dried oregano, salt, and pepper for dressing.", null),
                    RecipeInstruction(3, "Pour dressing over salad, toss gently, and top with crumbled feta cheese.", null)
                ),
                tips = listOf("Add grilled chicken or shrimp for extra protein."),
                equipment = listOf("Salad Bowl", "Whisk")
            )
        )
    }
}

class PantryRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    val pantryItems: Flow<List<PantryItem>> = database.pantryDao().getAllPantryItems()

    suspend fun seedInitialPantryIfEmpty(currentItems: List<PantryItem>) = withContext(Dispatchers.IO) {
        if (currentItems.isEmpty()) {
            val defaults = listOf(
                PantryItem("p_01", "Extra Virgin Olive Oil", 1.0, "bottle", "Spices & Oils"),
                PantryItem("p_02", "Garlic Cloves", 6.0, "cloves", "Produce"),
                PantryItem("p_03", "Large Eggs", 12.0, "units", "Dairy & Eggs"),
                PantryItem("p_04", "Spaghetti Pasta", 500.0, "g", "Pantry"),
                PantryItem("p_05", "Sea Salt & Black Pepper", 1.0, "set", "Spices & Oils"),
                PantryItem("p_06", "Butter", 200.0, "g", "Dairy & Eggs"),
                PantryItem("p_07", "Chicken Breast", 400.0, "g", "Meat & Seafood")
            )
            database.pantryDao().insertPantryItems(defaults)
        }
    }

    suspend fun addPantryItem(name: String, amount: Double = 1.0, unit: String = "item", category: String = "Pantry") = withContext(Dispatchers.IO) {
        val item = PantryItem(
            id = "pant_" + UUID.randomUUID().toString().take(6),
            name = name,
            amount = amount,
            unit = unit,
            category = category
        )
        database.pantryDao().insertPantryItem(item)
    }

    suspend fun deletePantryItem(id: String) = withContext(Dispatchers.IO) {
        database.pantryDao().deletePantryItem(id)
    }

    suspend fun clearPantry() = withContext(Dispatchers.IO) {
        database.pantryDao().clearPantry()
    }
}

class ShoppingRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    val shoppingItems: Flow<List<ShoppingItem>> = database.shoppingDao().getAllItems()

    suspend fun addItem(name: String, amount: Double = 1.0, unit: String = "item", category: String = "Pantry", sourceRecipe: String? = null) = withContext(Dispatchers.IO) {
        val item = ShoppingItem(
            id = "shop_" + UUID.randomUUID().toString().take(6),
            ingredientName = name,
            amount = amount,
            unit = unit,
            category = category,
            isCompleted = false,
            recipeSourceTitle = sourceRecipe
        )
        database.shoppingDao().insertItem(item)
    }

    suspend fun addRecipeIngredients(recipe: Recipe) = withContext(Dispatchers.IO) {
        val items = recipe.ingredients.map { ing ->
            ShoppingItem(
                id = "shop_" + UUID.randomUUID().toString().take(6),
                ingredientName = ing.name,
                amount = ing.amount,
                unit = ing.unit,
                category = ing.category,
                isCompleted = false,
                recipeSourceTitle = recipe.title
            )
        }
        database.shoppingDao().insertItems(items)
    }

    suspend fun toggleItem(id: String, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        database.shoppingDao().updateCompletionStatus(id, isCompleted)
    }

    suspend fun deleteItem(id: String) = withContext(Dispatchers.IO) {
        database.shoppingDao().deleteItem(id)
    }

    suspend fun clearCompleted() = withContext(Dispatchers.IO) {
        database.shoppingDao().clearCompletedItems()
    }

    suspend fun transferCompletedToPantry(pantryRepository: PantryRepository, completedItems: List<ShoppingItem>) = withContext(Dispatchers.IO) {
        completedItems.filter { it.isCompleted }.forEach { item ->
            pantryRepository.addPantryItem(
                name = item.ingredientName,
                amount = item.amount,
                unit = item.unit,
                category = item.category
            )
        }
        database.shoppingDao().clearCompletedItems()
    }
}

class MealPlanRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    val allMealPlans: Flow<List<MealPlanItem>> = database.mealPlanDao().getAllMealPlans()

    fun getMealPlansForDate(dateString: String): Flow<List<MealPlanItem>> {
        return database.mealPlanDao().getMealPlansForDate(dateString)
    }

    suspend fun addRecipeToMealPlan(dateString: String, mealType: String, recipe: Recipe, servesWho: String = "Family") = withContext(Dispatchers.IO) {
        val item = MealPlanItem(
            id = "plan_" + UUID.randomUUID().toString().take(6),
            dateString = dateString,
            mealType = mealType,
            recipeId = recipe.id,
            recipeTitle = recipe.title,
            recipeImageUrl = recipe.imageUrl,
            calories = recipe.calories,
            prepTimeMinutes = recipe.totalTimeMinutes,
            servesWho = servesWho
        )
        database.mealPlanDao().insertMealPlan(item)
    }

    suspend fun removeMealPlan(id: String) = withContext(Dispatchers.IO) {
        database.mealPlanDao().deleteMealPlan(id)
    }

    suspend fun exportMealPlanToShoppingList(
        plans: List<MealPlanItem>,
        recipes: List<Recipe>,
        shoppingRepository: ShoppingRepository
    ) = withContext(Dispatchers.IO) {
        val recipeMap = recipes.associateBy { it.id }
        plans.forEach { plan ->
            val recipe = recipeMap[plan.recipeId]
            if (recipe != null) {
                shoppingRepository.addRecipeIngredients(recipe)
            } else {
                shoppingRepository.addItem(plan.recipeTitle, 1.0, "servings", "Meal Plan", plan.recipeTitle)
            }
        }
    }
}

class UserRepository(private val context: Context) {
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile

    fun updateProfile(
        name: String? = null,
        email: String? = null,
        measurementUnit: String? = null,
        dietaryPreferences: List<String>? = null,
        allergies: List<String>? = null,
        notificationsEnabled: Boolean? = null,
        nutritionTarget: NutritionTarget? = null
    ) {
        _userProfile.value = _userProfile.value.copy(
            name = name ?: _userProfile.value.name,
            email = email ?: _userProfile.value.email,
            measurementUnit = measurementUnit ?: _userProfile.value.measurementUnit,
            dietaryPreferences = dietaryPreferences ?: _userProfile.value.dietaryPreferences,
            allergies = allergies ?: _userProfile.value.allergies,
            notificationsEnabled = notificationsEnabled ?: _userProfile.value.notificationsEnabled,
            nutritionTarget = nutritionTarget ?: _userProfile.value.nutritionTarget
        )
    }

    fun logout() {
        _userProfile.value = _userProfile.value.copy(isLoggedIn = false)
    }

    fun login() {
        _userProfile.value = _userProfile.value.copy(isLoggedIn = true)
    }
}
