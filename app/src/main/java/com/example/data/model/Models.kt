package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val author: String = "RecipeAI Chef",
    val rating: Double = 4.8,
    val reviewCount: Int = 124,
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,
    val totalTimeMinutes: Int = prepTimeMinutes + cookTimeMinutes,
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val servings: Int = 4,
    val calories: Int = 450,
    val proteinGrams: Int = 30,
    val carbsGrams: Int = 40,
    val fatGrams: Int = 15,
    val category: String = "Dinner",
    val cuisine: String = "International",
    val imageUrl: String = "",
    val ingredients: List<RecipeIngredient> = emptyList(),
    val instructions: List<RecipeInstruction> = emptyList(),
    val tips: List<String> = emptyList(),
    val equipment: List<String> = emptyList(),
    val dietaryTags: List<String> = emptyList(),
    val isFavorite: Boolean = false
)

data class RecipeIngredient(
    val name: String,
    val amount: Double,
    val unit: String,
    val category: String = "Pantry"
)

data class RecipeInstruction(
    val stepNumber: Int,
    val description: String,
    val timerMinutes: Int? = null
)

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey val id: String,
    val ingredientName: String,
    val amount: Double,
    val unit: String,
    val category: String,
    val isCompleted: Boolean = false,
    val recipeSourceTitle: String? = null
)

@Entity(tableName = "meal_plan_items")
data class MealPlanItem(
    @PrimaryKey val id: String,
    val dateString: String, // YYYY-MM-DD format
    val mealType: String, // Breakfast, Lunch, Dinner, Snack
    val recipeId: String,
    val recipeTitle: String,
    val recipeImageUrl: String,
    val calories: Int,
    val prepTimeMinutes: Int,
    val servesWho: String = "Family"
)

@Entity(tableName = "favorite_recipes")
data class FavoriteRecipeEntity(
    @PrimaryKey val id: String,
    val jsonContent: String,
    val timestamp: Long = System.currentTimeMillis(),
    val collectionName: String = "General Favorites"
)

@Entity(tableName = "recipe_collections")
data class RecipeCollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconName: String = "folder"
)

@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey val id: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val category: String,
    val addedDate: Long = System.currentTimeMillis()
)

data class NutritionTarget(
    val dailyCalories: Int = 2000,
    val dailyProteinGrams: Int = 130,
    val dailyCarbsGrams: Int = 220,
    val dailyFatGrams: Int = 65
)

data class UserProfile(
    val userId: String = "user_demo_101",
    val name: String = "Noah Lopez",
    val email: String = "noah.allen.lopez1@gmail.com",
    val photoUrl: String = "",
    val measurementUnit: String = "Metric", // Metric or Imperial
    val dietaryPreferences: List<String> = listOf("High Protein", "Healthy"),
    val allergies: List<String> = listOf("Peanuts"),
    val notificationsEnabled: Boolean = true,
    val isLoggedIn: Boolean = true,
    val nutritionTarget: NutritionTarget = NutritionTarget()
)

data class CategoryItem(
    val name: String,
    val iconName: String,
    val isPopular: Boolean = false
)
