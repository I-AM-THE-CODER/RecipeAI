package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY isCompleted ASC, category ASC, ingredientName ASC")
    fun getAllItems(): Flow<List<ShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ShoppingItem>)

    @Query("UPDATE shopping_items SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompletionStatus(id: String, isCompleted: Boolean)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM shopping_items WHERE isCompleted = 1")
    suspend fun clearCompletedItems()

    @Query("DELETE FROM shopping_items")
    suspend fun clearAll()
}

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM meal_plan_items ORDER BY dateString ASC, mealType ASC")
    fun getAllMealPlans(): Flow<List<MealPlanItem>>

    @Query("SELECT * FROM meal_plan_items WHERE dateString = :date ORDER BY mealType ASC")
    fun getMealPlansForDate(date: String): Flow<List<MealPlanItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(item: MealPlanItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlans(items: List<MealPlanItem>)

    @Query("DELETE FROM meal_plan_items WHERE id = :id")
    suspend fun deleteMealPlan(id: String)

    @Query("DELETE FROM meal_plan_items WHERE dateString = :date")
    suspend fun deleteMealPlansForDate(date: String)
}

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorite_recipes ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteRecipeEntity>>

    @Query("SELECT * FROM favorite_recipes WHERE id = :id LIMIT 1")
    suspend fun getFavoriteById(id: String): FavoriteRecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteRecipeEntity)

    @Query("DELETE FROM favorite_recipes WHERE id = :id")
    suspend fun deleteFavorite(id: String)
}

@Dao
interface CollectionDao {
    @Query("SELECT * FROM recipe_collections ORDER BY name ASC")
    fun getAllCollections(): Flow<List<RecipeCollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: RecipeCollectionEntity)

    @Query("DELETE FROM recipe_collections WHERE id = :id")
    suspend fun deleteCollection(id: String)
}

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY category ASC, name ASC")
    fun getAllPantryItems(): Flow<List<PantryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPantryItem(item: PantryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPantryItems(items: List<PantryItem>)

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deletePantryItem(id: String)

    @Query("DELETE FROM pantry_items")
    suspend fun clearPantry()
}

@Database(
    entities = [ShoppingItem::class, MealPlanItem::class, FavoriteRecipeEntity::class, RecipeCollectionEntity::class, PantryItem::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingDao(): ShoppingDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun collectionDao(): CollectionDao
    abstract fun pantryDao(): PantryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recipeai_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
