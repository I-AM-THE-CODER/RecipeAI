package com.example.ui.profile

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.UserRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userRepository: UserRepository,
    modifier: Modifier = Modifier
) {
    val userProfile by userRepository.userProfile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(userProfile.name) }
    var editEmail by remember { mutableStateOf(userProfile.email) }

    val allDiets = listOf("Vegetarian", "Vegan", "Gluten Free", "Dairy Free", "High Protein", "Low Carb", "Keto")
    val allAllergies = listOf("Peanuts", "Tree Nuts", "Dairy", "Shellfish", "Soy", "Eggs")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .padding(bottom = 80.dp)
        ) {
            // User Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfile.name.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userProfile.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AssistChip(
                            onClick = {},
                            label = { Text(if (userProfile.isLoggedIn) "Firebase Authenticated" else "Guest Mode") },
                            leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }

                    IconButton(onClick = {
                        editName = userProfile.name
                        editEmail = userProfile.email
                        showEditProfileDialog = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dietary Preferences Settings
            Text("Dietary Preferences", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    allDiets.forEach { diet ->
                        val isSelected = userProfile.dietaryPreferences.contains(diet)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = userProfile.dietaryPreferences.toMutableList()
                                    if (isSelected) current.remove(diet) else current.add(diet)
                                    userRepository.updateProfile(dietaryPreferences = current)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(diet, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    val current = userProfile.dietaryPreferences.toMutableList()
                                    if (checked) current.add(diet) else current.remove(diet)
                                    userRepository.updateProfile(dietaryPreferences = current)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Allergy Checklist Settings
            Text("Allergies & Sensitivities", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    allAllergies.forEach { allergy ->
                        val isHas = userProfile.allergies.contains(allergy)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = userProfile.allergies.toMutableList()
                                    if (isHas) current.remove(allergy) else current.add(allergy)
                                    userRepository.updateProfile(allergies = current)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(allergy, fontWeight = FontWeight.Medium)
                            Checkbox(
                                checked = isHas,
                                onCheckedChange = { checked ->
                                    val current = userProfile.allergies.toMutableList()
                                    if (checked == true) current.add(allergy) else current.remove(allergy)
                                    userRepository.updateProfile(allergies = current)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nutrition Targets Card
            Text("Daily Nutrition Targets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            var editCalories by remember { mutableStateOf(userProfile.nutritionTarget.dailyCalories.toString()) }
            var editProtein by remember { mutableStateOf(userProfile.nutritionTarget.dailyProteinGrams.toString()) }
            var editCarbs by remember { mutableStateOf(userProfile.nutritionTarget.dailyCarbsGrams.toString()) }
            var editFat by remember { mutableStateOf(userProfile.nutritionTarget.dailyFatGrams.toString()) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editCalories,
                            onValueChange = { editCalories = it },
                            label = { Text("Calories (kcal)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editProtein,
                            onValueChange = { editProtein = it },
                            label = { Text("Protein (g)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editCarbs,
                            onValueChange = { editCarbs = it },
                            label = { Text("Carbs (g)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editFat,
                            onValueChange = { editFat = it },
                            label = { Text("Fat (g)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            val newTarget = com.example.data.model.NutritionTarget(
                                dailyCalories = editCalories.toIntOrNull() ?: 2000,
                                dailyProteinGrams = editProtein.toIntOrNull() ?: 130,
                                dailyCarbsGrams = editCarbs.toIntOrNull() ?: 220,
                                dailyFatGrams = editFat.toIntOrNull() ?: 65
                            )
                            userRepository.updateProfile(nutritionTarget = newTarget)
                            scope.launch { snackbarHostState.showSnackbar("Updated Daily Macro Targets! 🎯") }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Macro Goals")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Measurement System Unit Toggle
            Text("Units of Measurement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("System Units", fontWeight = FontWeight.Bold)
                        Text("grams, ml vs cups, oz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = userProfile.measurementUnit == "Metric",
                            onClick = { userRepository.updateProfile(measurementUnit = "Metric") },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) {
                            Text("Metric")
                        }
                        SegmentedButton(
                            selected = userProfile.measurementUnit == "Imperial",
                            onClick = { userRepository.updateProfile(measurementUnit = "Imperial") },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) {
                            Text("Imperial")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Account Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextButton(
                        onClick = {
                            if (userProfile.isLoggedIn) userRepository.logout() else userRepository.login()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                            Icon(if (userProfile.isLoggedIn) Icons.Default.Logout else Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(if (userProfile.isLoggedIn) "Log Out" else "Log In")
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    TextButton(
                        onClick = {
                            userRepository.updateProfile(name = "User", email = "")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Delete Account & Data", color = Color.Red)
                        }
                    }
                }
            }
        }

        // Edit Profile Dialog
        if (showEditProfileDialog) {
            AlertDialog(
                onDismissRequest = { showEditProfileDialog = false },
                title = { Text("Edit User Profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            userRepository.updateProfile(name = editName, email = editEmail)
                            showEditProfileDialog = false
                        }
                    ) {
                        Text("Save Profile")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
