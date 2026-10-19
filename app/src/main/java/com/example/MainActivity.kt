package com.example

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavHostController
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.HabitRepository
import com.example.data.SettingsManager
import com.example.ui.HabitViewModel
import com.example.ui.HabitViewModelFactory
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize SQLite persistence layers
        val database = AppDatabase.getDatabase(this)
        val repository = HabitRepository(database.habitDao())
        
        // Initialize client preferences manager
        val settingsManager = SettingsManager(this)

        // Initialize core application state manager
        val factory = HabitViewModelFactory(application, repository, settingsManager)
        val viewModel = ViewModelProvider(this, factory)[HabitViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isOnboarded by viewModel.isOnboarded.collectAsState()

            // Map themeMode (0 = system, 1 = light, 2 = dark)
            val useDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                val internalNavController = rememberNavController()
                this@MainActivity.navController = internalNavController
                val navController = internalNavController

                // Cold start: read once, after the NavHost/NavController exist and onboarding is confirmed done
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    intent.getStringExtra("widget_destination")?.let { dest ->
                        if (settingsManager.isOnboarded.value) {
                            internalNavController.navigate(dest) { launchSingleTop = true }
                        }
                    }
                }

                // Decide start destination based on onboarding memory
                val startDestination = if (isOnboarded) "today" else "onboarding"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (isOnboarded) {
                            AppBottomNavBar(navController = navController, viewModel = viewModel)
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                viewModel = viewModel,
                                onOnboardingComplete = {
                                    navController.navigate("today") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("today") {
                            TodayScreen(
                                viewModel = viewModel
                            )
                        }
                        composable("all_habits") {
                            AllHabitsScreen(
                                viewModel = viewModel
                            )
                        }
                        composable("streaks") {
                            StreaksProgressScreen(
                                viewModel = viewModel
                            )
                        }
                        composable("reflection") {
                            ReflectionScreen(
                                viewModel = viewModel
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }

    // Warm start: the activity already exists, so onCreate won't re-run — handle the fresh intent here
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("widget_destination")?.let { dest ->
            navController?.navigate(dest) { launchSingleTop = true }
        }
    }
}

@Composable
fun AppBottomNavBar(navController: androidx.navigation.NavHostController, viewModel: HabitViewModel) {
    val context = LocalContext.current
    val items = listOf(
        Triple("today", Icons.Default.Home, "Today"),
        Triple("all_habits", Icons.Default.List, "My Habits"),
        Triple("streaks", Icons.Default.Star, "Streaks"),
        Triple("reflection", Icons.Default.Edit, "Reflection"),
        Triple("settings", Icons.Default.Settings, "Settings")
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        items.forEach { (route, icon, label) ->
            val isSel = currentRoute == route
            NavigationBarItem(
                selected = isSel,
                onClick = {
                    if (currentRoute != route) {
                        viewModel.triggerTapHaptic(context)
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    indicatorColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}
