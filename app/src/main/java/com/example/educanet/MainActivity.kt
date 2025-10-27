package com.example.educanet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { EducanetNav() } }
    }
}

@Composable
fun EducanetNav() {
    val nav = rememberNavController()
    val auth = FirebaseAuth.getInstance()

    NavHost(navController = nav, startDestination = "splash") {
        composable("splash") {
            SplashScreen {
                val start = if (auth.currentUser != null) "home" else "login"
                nav.navigate(start) { popUpTo("splash") { inclusive = true } }
            }
        }
        composable("login") {
            LoginScreen(
                onGoRegister = { nav.navigate("register") },
                onSuccess = { nav.navigate("home") { popUpTo("login") { inclusive = true } } }
            )
        }
        composable("register") {
            RegisterScreen(
                onGoLogin = { nav.popBackStack() },
                onSuccess = { nav.navigate("home") { popUpTo("login") { inclusive = true } } }
            )
        }
        composable("home") {
            HomeScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    nav.navigate("login") { popUpTo("home") { inclusive = true } }
                },
                onNewClass = { nav.navigate("createClass") },
                onOpenClass = { id -> nav.navigate("classDetail/$id") }
            )
        }
        composable("createClass") {
            CreateClassScreen(onDone = { nav.popBackStack() })
        }
        composable("classDetail/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            ClassDetailScreen(classId = id, onBack = { nav.popBackStack() })
        }
    }
}
