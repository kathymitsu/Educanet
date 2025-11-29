package com.example.educanet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.educanet.screen.CartScreen
import com.example.educanet.screen.ClassDetailScreen
import com.example.educanet.screen.HomeScreen
import com.example.educanet.screen.ProgressScreen
import com.example.educanet.screen.SettingsScreen
// importa también tus otras pantallas si las necesitas:
// import com.example.educanet.screen.CreateClassScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EducanetApp()
        }
    }
}

@Composable
fun EducanetApp() {
    val navController = rememberNavController()

    MaterialTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { /* usamos TopAppBar dentro de cada pantalla, así que nada aquí */ }
        ) { innerPadding ->
            EducanetNav(
                navController = navController,
                innerPadding = innerPadding
            )
        }
    }
}

@Composable
fun EducanetNav(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier
            .fillMaxSize()
    ) {
        // HOME
        composable(route = "home") {
            HomeScreen(
                onLogout = {
                    // aquí puedes hacer signOut de Firebase si quieres
                    // FirebaseAuth.getInstance().signOut()
                    // y quizá navegar a una pantalla de login
                },
                onNewClass = {
                    navController.navigate("createClass")
                },
                onOpenClass = { classId ->
                    navController.navigate("classDetail/$classId")
                },
                onOpenResources = {
                    // si tienes otra pantalla de recursos, navega aquí
                    // navController.navigate("resources")
                },
                onOpenProgress = { studentId ->
                    navController.navigate("progress/$studentId")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                },
                onOpenCart = {                    // 👈 ESTE ES EL QUE FALTABA
                    navController.navigate("cart")
                }
            )
        }

        // DETALLE DE CLASE + NOTAS + RESEÑAS
        composable(
            route = "classDetail/{classId}",
            arguments = listOf(
                navArgument("classId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            ClassDetailScreen(
                classId = classId,
                onBack = { navController.popBackStack() }
            )
        }

        // PROGRESO (alumno / apoderado)
        composable(
            route = "progress/{studentId}",
            arguments = listOf(
                navArgument("studentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId")
            ProgressScreen(
                onBack = { navController.popBackStack() },
                studentId = studentId
            )
        }

        // AJUSTES (avatar, cámara + galería)
        composable(route = "settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // CARRITO
        composable(route = "cart") {
            CartScreen(
                onBack = { navController.popBackStack() },
                onCheckoutSuccess = {
                    // después de confirmar inscripción, volvemos atrás
                    navController.popBackStack()
                }
            )
        }

        // CREAR CLASE (si tienes esta pantalla)
        composable(route = "createClass") {
            // Descomenta y ajusta si tienes CreateClassScreen
            /*
            CreateClassScreen(
                onCancel = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
            */
        }
    }
}
