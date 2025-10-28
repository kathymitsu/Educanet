package com.example.educanet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument           // <- importante: de androidx.navigation, no .compose
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    EducanetNav()
                }
            }
        }
    }
}

@Composable
fun EducanetNav() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val start = if (auth.currentUser == null) "login" else "home"

    NavHost(navController = navController, startDestination = start) {

        // 🔐 Login (usa tu firma onSuccess/onGoRegister)
        composable("login") {
            LoginScreen(
                onSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onGoRegister = { navController.navigate("register") }
            )
        }

        // 📝 Registro (usa tu firma onSuccess/onGoLogin)
        composable("register") {
            RegisterScreen(
                onSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onGoLogin = { navController.popBackStack() }
            )
        }

        // 🏠 Home
        composable("home") {
            HomeScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNewClass = { navController.navigate("createClass") },
                onOpenClass = { id -> navController.navigate("classDetail/$id") },
                onOpenResources = { navController.navigate("resources") },
                onOpenProgress = { navController.navigate("progress") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }

        // ➕ Crear clase
        composable("createClass") {
            CreateClassScreen(
                onCancel = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // 📄 Detalle clase
        composable(
            route = "classDetail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStack ->
            val classId = backStack.arguments?.getString("id") ?: ""
            ClassDetailScreen(
                classId = classId,
                onBack = { navController.popBackStack() }
            )
        }

        // 📚 Recursos
        composable("resources") {
            ResourcesScreen(onBack = { navController.popBackStack() })
        }

        // 📈 Progreso
        composable("progress") {
            ProgressScreen(onBack = { navController.popBackStack() })
        }

        // ⚙️ Ajustes (DataStore + avatar local)
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
