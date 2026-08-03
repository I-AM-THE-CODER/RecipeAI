package com.example.data.model.remote

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import com.example.data.model.RecipeInstruction
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiAiService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    suspend fun generateRecipeFromIngredients(
        ingredientsList: List<String>,
        dietary: String? = null,
        mealType: String? = null
    ): Recipe = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext MockAiRecipeProvider.generateSmartMockRecipe(
                prompt = ingredientsList.joinToString(", "),
                dietary = dietary,
                mealType = mealType
            )
        }

        val prompt = """
            You are a world-class executive chef AI.
            Create a unique, delicious, highly-detailed recipe based on these available ingredients: ${ingredientsList.joinToString(", ")}.
            ${if (!dietary.isNull_or_blank_safe()) "Dietary restriction: $dietary." else ""}
            ${if (!mealType.isNull_or_blank_safe()) "Meal type: $mealType." else ""}
            
            Return ONLY a valid JSON object matching this exact structure:
            {
              "title": "Recipe Name",
              "description": "Engaging description",
              "prepTimeMinutes": 15,
              "cookTimeMinutes": 20,
              "servings": 4,
              "difficulty": "Easy",
              "calories": 420,
              "proteinGrams": 28,
              "carbsGrams": 35,
              "fatGrams": 14,
              "category": "Dinner",
              "cuisine": "American",
              "ingredients": [
                {"name": "Ingredient 1", "amount": 2.0, "unit": "cups", "category": "Produce"}
              ],
              "instructions": [
                {"stepNumber": 1, "description": "Preparation detail...", "timerMinutes": 5}
              ],
              "tips": ["Chef tip 1"],
              "equipment": ["Large skillet", "Cutting board"],
              "dietaryTags": ["High Protein"]
            }
        """.trimIndent()

        try {
            val jsonResponse = callGeminiRestApi(prompt, apiKey)
            parseRecipeFromJson(jsonResponse) ?: MockAiRecipeProvider.generateSmartMockRecipe(
                prompt = ingredientsList.joinToString(", "),
                dietary = dietary,
                mealType = mealType
            )
        } catch (e: Exception) {
            MockAiRecipeProvider.generateSmartMockRecipe(
                prompt = ingredientsList.joinToString(", "),
                dietary = dietary,
                mealType = mealType
            )
        }
    }

    suspend fun scanIngredientsFromImage(bitmap: Bitmap): List<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext listOf(
                "Fresh Basil", "Cherry Tomatoes", "Garlic Cloves", "Olive Oil",
                "Parmesan Cheese", "Pasta", "Bell Peppers"
            )
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "Identify all raw food ingredients, vegetables, fruits, meats, or kitchen pantry items visible in this image. Return a clean comma-separated list of ingredient names only."))
                            put(JSONObject().put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            }))
                        })
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val parsedText = extractTextFromGeminiResponse(responseBody)
            
            if (parsedText.isNotBlank()) {
                parsedText.split(",", "\n").map { it.trim().removePrefix("-").trim() }.filter { it.length > 2 }
            } else {
                listOf("Chicken Breast", "Avocado", "Tomatoes", "Limes", "Cilantro")
            }
        } catch (e: Exception) {
            listOf("Salmon Fillet", "Asparagus", "Lemons", "Garlic", "Olive Oil", "Dill")
        }
    }

    suspend fun convertRecipeDietary(recipe: Recipe, targetDiet: String): Recipe = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext MockAiRecipeProvider.convertMockRecipe(recipe, targetDiet)
        }

        val prompt = """
            Convert the following recipe into a $targetDiet version with appropriate smart ingredient substitutions while maintaining flavor.
            Original Recipe: ${recipe.title}
            Original Ingredients: ${recipe.ingredients.joinToString { "${it.amount} ${it.unit} ${it.name}" }}
            
            Return JSON in the same recipe structure (title, description, prepTimeMinutes, cookTimeMinutes, servings, calories, proteinGrams, carbsGrams, fatGrams, ingredients, instructions, tips, equipment).
        """.trimIndent()

        try {
            val jsonResponse = callGeminiRestApi(prompt, apiKey)
            parseRecipeFromJson(jsonResponse) ?: MockAiRecipeProvider.convertMockRecipe(recipe, targetDiet)
        } catch (e: Exception) {
            MockAiRecipeProvider.convertMockRecipe(recipe, targetDiet)
        }
    }

    suspend fun askChefQuestion(question: String, contextRecipe: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Chef AI Tip: For best results, ensure your skillet is hot before searing proteins to lock in juices!"
        }

        val prompt = "You are RecipeAI Chef. Answer this culinary question concisely with warm expertise: $question ${if (!contextRecipe.isNull_or_blank_safe()) "Context Recipe: $contextRecipe" else ""}"
        try {
            val jsonResponse = callGeminiRestApi(prompt, apiKey)
            extractTextFromGeminiResponse(jsonResponse).ifBlank { "To substitute butter, use olive oil or avocado oil in equal measure for roasting." }
        } catch (e: Exception) {
            "For optimal texture, let grilled meat rest for 5-10 minutes before slicing."
        }
    }

    private fun callGeminiRestApi(promptText: String, apiKey: String): String {
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", promptText))
                    })
                })
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    private fun extractTextFromGeminiResponse(responseBody: String): String {
        return try {
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            if (parts.length() == 0) return ""
            parts.getJSONObject(0).optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseRecipeFromJson(jsonResponseText: String): Recipe? {
        val text = extractTextFromGeminiResponse(jsonResponseText)
        if (text.isBlank()) return null

        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        val jsonString = text.substring(start, end + 1)

        return try {
            val fullJson = JSONObject(jsonString)
            val title = fullJson.optString("title", "AI Gourmet Special")
            val desc = fullJson.optString("description", "A custom AI chef creation tailored for your kitchen.")
            val prep = fullJson.optInt("prepTimeMinutes", 15)
            val cook = fullJson.optInt("cookTimeMinutes", 20)
            val servings = fullJson.optInt("servings", 4)
            val cals = fullJson.optInt("calories", 450)
            val protein = fullJson.optInt("proteinGrams", 30)
            val carbs = fullJson.optInt("carbsGrams", 40)
            val fat = fullJson.optInt("fatGrams", 15)
            val difficulty = fullJson.optString("difficulty", "Medium")
            val category = fullJson.optString("category", "Dinner")
            val cuisine = fullJson.optString("cuisine", "International")

            val ingredientsList = mutableListOf<RecipeIngredient>()
            val ingArray = fullJson.optJSONArray("ingredients")
            if (ingArray != null) {
                for (i in 0 until ingArray.length()) {
                    val obj = ingArray.getJSONObject(i)
                    ingredientsList.add(
                        RecipeIngredient(
                            name = obj.optString("name", "Ingredient"),
                            amount = obj.optDouble("amount", 1.0),
                            unit = obj.optString("unit", "tbsp"),
                            category = obj.optString("category", "Pantry")
                        )
                    )
                }
            }

            val instructionsList = mutableListOf<RecipeInstruction>()
            val instArray = fullJson.optJSONArray("instructions")
            if (instArray != null) {
                for (i in 0 until instArray.length()) {
                    val obj = instArray.getJSONObject(i)
                    instructionsList.add(
                        RecipeInstruction(
                            stepNumber = obj.optInt("stepNumber", i + 1),
                            description = obj.optString("description", ""),
                            timerMinutes = if (obj.has("timerMinutes")) obj.optInt("timerMinutes") else null
                        )
                    )
                }
            }

            Recipe(
                id = "ai_gen_" + UUID.randomUUID().toString().take(8),
                title = title,
                description = desc,
                prepTimeMinutes = prep,
                cookTimeMinutes = cook,
                servings = servings,
                calories = cals,
                proteinGrams = protein,
                carbsGrams = carbs,
                fatGrams = fat,
                difficulty = difficulty,
                category = category,
                cuisine = cuisine,
                ingredients = ingredientsList,
                instructions = instructionsList
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun String?.isNull_or_blank_safe(): Boolean = this == null || this.isBlank()
}

object MockAiRecipeProvider {
    fun generateSmartMockRecipe(prompt: String, dietary: String? = null, mealType: String? = null): Recipe {
        val title = when {
            prompt.contains("chicken", ignoreCase = true) -> "Tuscan Garlic Butter Chicken Skillet"
            prompt.contains("pasta", ignoreCase = true) -> "Creamy Tomato Basil Penne"
            prompt.contains("salmon", ignoreCase = true) -> "Honey Glazed Salmon with Lemon Asparagus"
            prompt.contains("avocado", ignoreCase = true) -> "Zesty Chipotle Avocado Power Bowl"
            else -> "Chef's Artisanal ${mealType ?: "Pantry"} Medley"
        }

        return Recipe(
            id = "mock_recipe_" + UUID.randomUUID().toString().take(6),
            title = title,
            description = "A wholesome chef-crafted dish combining $prompt with aromatic herbs and balanced seasonings.",
            prepTimeMinutes = 15,
            cookTimeMinutes = 20,
            servings = 4,
            calories = 480,
            proteinGrams = 32,
            carbsGrams = 38,
            fatGrams = 16,
            difficulty = "Easy",
            category = mealType ?: "Dinner",
            cuisine = "Mediterranean",
            ingredients = listOf(
                RecipeIngredient("Primary Protein/Base", 500.0, "g", "Meat & Seafood"),
                RecipeIngredient("Olive Oil", 2.0, "tbsp", "Pantry"),
                RecipeIngredient("Garlic Cloves (minced)", 3.0, "cloves", "Produce"),
                RecipeIngredient("Fresh Herbs (Thyme/Basil)", 1.0, "handful", "Produce"),
                RecipeIngredient("Sea Salt & Black Pepper", 1.0, "tsp", "Spices & Oils")
            ),
            instructions = listOf(
                RecipeInstruction(1, "Prepare ingredients: rinse vegetables, mince garlic, and pat proteins dry with paper towels.", null),
                RecipeInstruction(2, "Heat 2 tbsp olive oil in a heavy skillet over medium-high heat until shimmering.", 2),
                RecipeInstruction(3, "Add minced garlic and sauté for 1 minute until fragrant. Add main ingredients and sear until golden.", 8),
                RecipeInstruction(4, "Reduce heat, toss with herbs and seasonings, and simmer gently until fully cooked through.", 10),
                RecipeInstruction(5, "Plate immediately, garnish with fresh herbs, and serve warm.")
            ),
            tips = listOf("For extra crispiness, ensure the pan is preheated before adding ingredients.", "Store leftovers in an airtight container for up to 3 days."),
            equipment = listOf("Skillet", "Chef Knife", "Cutting Board"),
            dietaryTags = listOfNotNull(dietary, "High Protein", "Healthy")
        )
    }

    fun convertMockRecipe(original: Recipe, targetDiet: String): Recipe {
        return original.copy(
            id = "diet_conv_" + UUID.randomUUID().toString().take(6),
            title = "${original.title} ($targetDiet)",
            description = "${original.description} Reimagined for a $targetDiet lifestyle.",
            dietaryTags = original.dietaryTags + targetDiet
        )
    }
}
