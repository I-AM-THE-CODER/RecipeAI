package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.repository.MealPlanRepository
import com.example.data.repository.RecipeRepository
import com.example.data.repository.ShoppingRepository
import com.example.data.repository.UserRepository
import com.example.ui.aistudio.AiStudioScreen
import com.example.ui.favorites.FavoritesScreen
import com.example.ui.home.HomeScreen
import com.example.ui.mealplanner.MealPlannerScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.recipe.RecipeDetailScreen
import com.example.ui.search.SearchScreen
import com.example.ui.shopping.ShoppingListScreen
import com.example.ui.theme.RecipeAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecipeAITheme {
                RecipeMainApp()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Search : Screen("search", "Explore", Icons.Default.Search)
    object AiStudio : Screen("ai_studio", "AI Studio", Icons.Default.AutoAwesome)
    object MealPlanner : Screen("meal_planner", "Meal Plan", Icons.Default.CalendarMonth)
    object Shopping : Screen("shopping", "Shopping", Icons.Default.ShoppingCart)
    object Favorites : Screen("favorites", "Saved", Icons.Default.Favorite)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

@Composable
fun RecipeMainApp() {
    val context = LocalContext.current
    val navController = rememberNavController()

    val recipeRepository = remember { RecipeRepository(context) }
    val shoppingRepository = remember { ShoppingRepository(context) }
    val mealPlanRepository = remember { MealPlanRepository(context) }
    val userRepository = remember { UserRepository(context) }
    val pantryRepository = remember { com.example.data.repository.PantryRepository(context) }

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Search,
        Screen.AiStudio,
        Screen.MealPlanner,
        Screen.Shopping,
        Screen.Favorites,
        Screen.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, fontWeight = FontWeight.SemiBold) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    recipeRepository = recipeRepository,
                    mealPlanRepository = mealPlanRepository,
                    onRecipeClick = { recipeId -> navController.navigate("recipe_detail/$recipeId") },
                    onSearchClick = { navController.navigate(Screen.Search.route) },
                    onAiStudioClick = { navController.navigate(Screen.AiStudio.route) },
                    onMealPlannerClick = { navController.navigate(Screen.MealPlanner.route) },
                    onShoppingListClick = { navController.navigate(Screen.Shopping.route) }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    recipeRepository = recipeRepository,
                    onRecipeClick = { recipeId -> navController.navigate("recipe_detail/$recipeId") },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.AiStudio.route) {
                AiStudioScreen(
                    recipeRepository = recipeRepository,
                    pantryRepository = pantryRepository,
                    onRecipeClick = { recipeId -> navController.navigate("recipe_detail/$recipeId") }
                )
            }

            composable(Screen.MealPlanner.route) {
                MealPlannerScreen(
                    mealPlanRepository = mealPlanRepository,
                    recipeRepository = recipeRepository,
                    shoppingRepository = shoppingRepository,
                    onRecipeClick = { recipeId -> navController.navigate("recipe_detail/$recipeId") }
                )
            }

            composable(Screen.Shopping.route) {
                ShoppingListScreen(
                    shoppingRepository = shoppingRepository,
                    pantryRepository = pantryRepository
                )
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    recipeRepository = recipeRepository,
                    onRecipeClick = { recipeId -> navController.navigate("recipe_detail/$recipeId") }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    userRepository = userRepository
                )
            }

            composable(
                route = "recipe_detail/{recipeId}",
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
                RecipeDetailScreen(
                    recipeId = recipeId,
                    recipeRepository = recipeRepository,
                    shoppingRepository = shoppingRepository,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

