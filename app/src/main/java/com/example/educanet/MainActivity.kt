package com.example.educanet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
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
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val start = if (auth.currentUser == null) "login" else "home"

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(innerPadding)
        ){

            // 🔐 Login (usa tu firma onSuccess/onGoRegister)
            composable("login") {
                LoginScreen(
                    onSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onGoRegister = { navController.navigate("register")},
                    snackbarHostState = snackbarHostState

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
                    onOpenProgress = { userId ->
                        navController.navigate("progress/$userId") },
                    onOpenSettings = {navController.navigate("Settings")}

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
            composable(
                route = "progress/{userId}", arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
               val userId = backStackEntry.arguments?.getString("userId")

                ProgressScreen(
                    onBack = { navController.popBackStack() },
                    studentId = userId
                )
            }

            // ⚙️ Ajustes (DataStore + avatar local)
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable("createClass") {
                CreateClassScreenAdmin(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable("createClassAdmin") {
                CreateClassScreenAdmin(
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack() // vuelve al Home
                        // opcional: mostrar snackbar o refresh
                    }
                )
            }


        }

    }
}
