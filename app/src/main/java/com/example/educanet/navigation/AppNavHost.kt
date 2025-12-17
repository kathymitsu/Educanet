
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
        // AppNavHost.kt

        composable("home") {
            // Inside composable("home") { ... }
            HomeScreen(
                onLogout = {
                    // ...
                },        onNewClass = { navController.navigate("classEdit") },
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
                onOpenMyClasses = {
                    navController.navigate("myClasses")
                },
                onOpenNotifications = {
                    navController.navigate("notifications")
                },
                onCreateProfessor = {
                    // You can define the navigation for creating a professor here.
                    // For example, navigate to a "createProfessor" screen.
                    // navController.navigate("createProfessor")
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
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate("classEdit/$id") } // <-- Añadido para editar

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

       // CREAR/EDITAR CLASE
        composable(
            route = "classEdit?classId={classId}",
            arguments = listOf(navArgument("classId") {
                type = NavType.StringType
                nullable = true
            })
        ) {
            ClassEditScreen(
                classId = it.arguments?.getString("classId"),
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "classEdit/{classId}",
            arguments = listOf(navArgument("classId") { type = NavType.StringType })
        ) {
            ClassEditScreen(
                classId = it.arguments?.getString("classId"),
                onBack = { navController.popBackStack() }
            )
        }


        // MIS CLASES (clases pagadas del alumno)
        composable("myClasses") {
            MyClassesScreen(
                onBack = { navController.popBackStack() },
                onOpenClass = { classId ->
                    navController.navigate("classDetail/$classId")        },
                onOpenProgress = { classId ->      //
                    navController.navigate("studentProgress/$classId")
                }
            )
        }

        // PROGRESO DE ALUMNOS POR CLASE
        composable(
            route = "studentProgress/{classId}",
            arguments = listOf(navArgument("classId") { type = NavType.StringType })
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            StudentProgressScreen(
                classId = classId,
                onBack = { navController.popBackStack() },
                onOpenProgress = { studentId ->
                    navController.navigate("progress/$studentId")
                }
            )
        }
    }
}
