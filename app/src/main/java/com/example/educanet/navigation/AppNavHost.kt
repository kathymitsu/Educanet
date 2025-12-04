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
        startDestination = "home"      // si quieres partir en login puedes cambiar a "login"
    ) {
        // HOME
        composable("home") {
            HomeScreen(
                onLogout = {
                    // aquí si quieres puedes limpiar sesión y navegar a login
                    // navController.navigate("login") {
                    //     popUpTo("home") { inclusive = true }
                    // }
                },
                onNewClass = { navController.navigate("createClass") },
                onOpenClass = { id -> navController.navigate("classDetail/$id") },
                onOpenResources = { /* navController.navigate("resources") si la creas */ },
                onOpenProgress = { studentId ->
                    navController.navigate("progress/$studentId")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                },
                onOpenCart = {
                    navController.navigate("cart")
                },
                onOpenMyClasses = {                      // 👈 FALTABA ESTE
                    navController.navigate("myClasses")
                }
            )
        }

        // DETALLE DE CLASE
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

        // PROGRESO
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

        // AJUSTES
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // CARRITO
        composable("cart") {
            CartScreen(
                onBack = { navController.popBackStack() },
                onCheckoutSuccess = { navController.popBackStack() }
            )
        }

        // CREAR CLASE
        composable("createClass") {
            CreateClassScreen(
                onCancel = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // MIS CLASES (clases pagadas del alumno)
        composable("myClasses") {
            MyClassesScreen(
                onBack = { navController.popBackStack() },
                onOpenClass = { classId ->
                    navController.navigate("classDetail/$classId")        },
                onOpenProgress = { studentId ->      //
                    navController.navigate("progress/$studentId")
                }
            )
        }
    }
}
