package com.example.educanet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.educanet.screen.AddResourceScreen
import com.example.educanet.screen.CartScreen
import com.example.educanet.screen.ClassDetailScreen
import com.example.educanet.screen.ClassEditScreen
import com.example.educanet.screen.CreateProfessorScreen
import com.example.educanet.screen.DeleteUserScreen
import com.example.educanet.screen.EvaluationEditScreen
import com.example.educanet.screen.EvaluationListScreen
import com.example.educanet.screen.EvaluationScreen
import com.example.educanet.screen.HomeScreen
import com.example.educanet.screen.LoginScreen
import com.example.educanet.screen.MyClassesScreen
import com.example.educanet.screen.ProgressScreen
import com.example.educanet.screen.RegisterScreen
import com.example.educanet.screen.SettingsScreen
import com.example.educanet.screen.ResourcesScreen
import com.example.educanet.ui.ui.EducanetTheme
import com.google.firebase.auth.FirebaseAuth

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

    EducanetTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            EducanetNav(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun EducanetNav(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = Modifier.fillMaxSize().then(modifier)
    ) {
        // LOGIN
        composable(route = "login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                },
                onRegisterClick = { navController.navigate("register") }
            )
        }

        // REGISTER
        composable(route = "register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                }
            )
        }

        // HOME
        composable(route = "home") {
            HomeScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") { popUpTo("home") { inclusive = true } }
                },
                onNewClass = { navController.navigate("classEdit") },
                onCreateProfessor = { navController.navigate("createProfessor") },
                onOpenClass = { classId -> navController.navigate("classDetail/$classId") },
                onOpenResources = { navController.navigate("resources") },
                onOpenProgress = { studentId -> navController.navigate("progress/$studentId") },
                onOpenSettings = { navController.navigate("settings") },
                onOpenCart = { navController.navigate("cart") },
                onOpenMyClasses = { navController.navigate("myClasses") },
                onOpenNotifications = {},
                onDeleteUser = { navController.navigate("deleteUser") }
            )
        }

        // DELETE USER
        composable("deleteUser") {
            DeleteUserScreen(onBack = { navController.popBackStack() })
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
                onEdit = { id -> navController.navigate("classEdit?classId=$id") },
                onOpenProgress = { studentId, progressClassId -> navController.navigate("progress/$studentId?classId=$progressClassId") },
                onOpenEvaluations = { evalClassId -> navController.navigate("evaluations/$evalClassId") }
            )
        }

        // CREAR/EDITAR CLASE (Unified)
        composable(
            route = "classEdit?classId={classId}",
            arguments = listOf(navArgument("classId") {
                type = NavType.StringType
                nullable = true
            })
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId")
            ClassEditScreen(
                classId = classId,
                onBack = { navController.popBackStack() }
            )
        }

        // PROGRESO
        composable(
            route = "progress/{studentId}?classId={classId}",
            arguments = listOf(
                navArgument("studentId") { type = NavType.StringType },
                navArgument("classId") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId")
            val classId = backStackEntry.arguments?.getString("classId")
            ProgressScreen(
                onBack = { navController.popBackStack() },
                studentId = studentId,
                classId = classId
            )
        }

        // AJUSTES
        composable(route = "settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        // CARRITO
        composable(route = "cart") {
            CartScreen(
                onBack = { navController.popBackStack() },
                onCheckoutSuccess = { navController.popBackStack() }
            )
        }

        // MIS CLASES (alumno)
        composable(route = "myClasses") {
            val studentId = FirebaseAuth.getInstance().currentUser?.uid
            MyClassesScreen(
                onBack = { navController.popBackStack() },
                onOpenClass = { classId -> navController.navigate("classDetail/$classId") },
                onOpenProgress = { classId ->
                    if (studentId != null) {
                        navController.navigate("progress/$studentId?classId=$classId")
                    }
                }
            )
        }

        // RECURSOS
        composable(route = "resources") {
            ResourcesScreen(
                onBack = { navController.popBackStack() },
                onAddResource = { navController.navigate("addResource") }
            )
        }

        // AÑADIR RECURSO
        composable(route = "addResource") {
            AddResourceScreen(onBack = { navController.popBackStack() })
        }
        
        // CREAR PROFESOR
        composable(route = "createProfessor") {
            CreateProfessorScreen(onBack = { navController.popBackStack() })
        }

        // EVALUACIONES
        composable(
            route = "evaluations/{classId}",
            arguments = listOf(navArgument("classId") { type = NavType.StringType })
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            EvaluationListScreen(
                classId = classId,
                onBack = { navController.popBackStack() },
                onOpenEvaluation = { evaluationId -> navController.navigate("evaluation/$evaluationId") },
                onNewEvaluation = { navController.navigate("evaluationEdit?classId=$classId") },
                onEditEvaluation = { evaluationId -> navController.navigate("evaluationEdit?classId=$classId&evaluationId=$evaluationId") }
            )
        }

        // CREAR/EDITAR EVALUACIÓN
        composable(
            route = "evaluationEdit?classId={classId}&evaluationId={evaluationId}",
            arguments = listOf(
                navArgument("classId") { type = NavType.StringType; nullable = true },
                navArgument("evaluationId") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId")
            val evaluationId = backStackEntry.arguments?.getString("evaluationId")
            EvaluationEditScreen(
                classId = classId,
                evaluationId = evaluationId,
                onBack = { navController.popBackStack() }
            )
        }

        // RESOLVER EVALUACIÓN
        composable(
            route = "evaluation/{evaluationId}",
            arguments = listOf(navArgument("evaluationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val evaluationId = backStackEntry.arguments?.getString("evaluationId") ?: ""
            EvaluationScreen(
                evaluationId = evaluationId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
