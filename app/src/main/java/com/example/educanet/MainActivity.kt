package com.example.educanet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.educanet.screen.CreateClassScreen
import com.example.educanet.screen.HomeScreen
import com.example.educanet.screen.LoginScreen
import com.example.educanet.screen.MyClassesScreen
import com.example.educanet.screen.ProgressScreen
import com.example.educanet.screen.RegisterScreen
import com.example.educanet.screen.SettingsScreen
import com.example.educanet.screen.ResourcesScreen

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
            modifier = Modifier.fillMaxSize()
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
        startDestination = "login",
        modifier = Modifier.fillMaxSize()
    ) {
        // LOGIN
        composable(route = "login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate("register")
                }
            )
        }

        // REGISTER
        composable(route = "register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // HOME
        composable(route = "home") {
            HomeScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNewClass = {
                    navController.navigate("createClass")
                },
                onOpenClass = { classId ->
                    navController.navigate("classDetail/$classId")
                },
                onOpenResources = {
                     navController.navigate("resources")
                },
                onOpenProgress = { studentId ->
                    navController.navigate("progress/$studentId")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                },
                onOpenCart = {
                    navController.navigate("cart")
                },
                onOpenMyClasses = {                      // 👈 NUEVO
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
        composable(route = "settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // CARRITO
        composable(route = "cart") {
            CartScreen(
                onBack = { navController.popBackStack() },
                onCheckoutSuccess = { navController.popBackStack() }
            )
        }

        // ✅ CREAR CLASE
        composable(route = "createClass") {
            CreateClassScreen(
                onCancel = { navController.popBackStack() },
                onSaved  = { navController.popBackStack() }
            )
        }

        // ✅ MIS CLASES (alumno)
        composable(route = "myClasses") {
            MyClassesScreen(
                onBack = { navController.popBackStack() },
                onOpenClass = { classId ->
                    navController.navigate("classDetail/$classId")
                },
                onOpenProgress = { studentId ->
                    navController.navigate("progress/$studentId")
                }
            )
        }

        // RECURSOS
        composable(route = "resources") {
            ResourcesScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
