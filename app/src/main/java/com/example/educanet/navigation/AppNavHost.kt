package com.example.educanet.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.educanet.screen.*

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onLogout = { /* tu lógica de logout */ },
                onNewClass = { navController.navigate("createClass") },
                onOpenClass = { id -> navController.navigate("classDetail/$id") },
                onOpenResources = { /* navegar a recursos si tienes */ },
                onOpenProgress = { studentId ->
                    navController.navigate("progress/$studentId")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                },
                onOpenCart = {
                    navController.navigate("cart")
                }
            )
        }

        composable(
            route = "classDetail/{classId}",
            arguments = listOf(navArgument("classId") { type = NavType.StringType })
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            ClassDetailScreen(
                classId = classId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "progress/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId")
            ProgressScreen(
                onBack = { navController.popBackStack() },
                studentId = studentId
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("cart") {
            CartScreen(
                onBack = { navController.popBackStack() },
                onCheckoutSuccess = { navController.popBackStack() }
            )
        }

        // Si tienes pantalla de creación de clase:
        // composable("createClass") { CreateClassScreen(... ) }
    }
}
