package nl.gymlog

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import nl.gymlog.ui.screens.CaptureScreen
import nl.gymlog.ui.screens.DetailScreen
import nl.gymlog.ui.screens.HomeScreen
import nl.gymlog.ui.screens.ReviewScreen
import nl.gymlog.ui.theme.GymLogTheme
import nl.gymlog.viewmodel.CaptureUiState
import nl.gymlog.viewmodel.CaptureViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymLogTheme {
                GymLogApp()
            }
        }
    }
}

@Composable
fun GymLogApp() {
    val navController = rememberNavController()
    val captureViewModel: CaptureViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToCapture = { navController.navigate("capture") },
                onNavigateToDetail = { metricKey ->
                    navController.navigate("detail/$metricKey")
                }
            )
        }

        composable("capture") {
            CaptureScreen(
                onImageCaptured = { bitmap: Bitmap ->
                    captureViewModel.processCapture(bitmap)
                    navController.navigate("review") {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("review") {
            val uiState by captureViewModel.uiState.collectAsState()

            // Handle OCR error: navigate back to capture
            if (uiState is CaptureUiState.Error) {
                // Show error inline via Toast-like state — handled in ReviewScreen
                // Nothing to navigate here; ReviewScreen shows the error
            }

            ReviewScreen(
                viewModel = captureViewModel,
                onSaved = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onRetake = {
                    navController.navigate("capture") {
                        popUpTo("capture") { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = "detail/{metricKey}",
            arguments = listOf(navArgument("metricKey") { type = NavType.StringType })
        ) { backStackEntry ->
            val metricKey = backStackEntry.arguments?.getString("metricKey") ?: "calories"
            DetailScreen(
                initialMetricKey = metricKey,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
