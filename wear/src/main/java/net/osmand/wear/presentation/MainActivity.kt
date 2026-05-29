package net.osmand.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import net.osmand.wear.data.OsmAndDataLayerClient
import net.osmand.wear.presentation.theme.AndroidTheme
import net.osmand.wear.ui.MainMenuScreen
import net.osmand.wear.ui.NavigationScreen
import net.osmand.wear.viewmodel.MainViewModel
import net.osmand.wear.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val dataLayerClient = OsmAndDataLayerClient(this)
        
        setContent {
            OsmAndWearApp(dataLayerClient)
        }
    }
}

@Composable
fun OsmAndWearApp(dataLayerClient: OsmAndDataLayerClient) {
    AndroidTheme {
        AppScaffold {
            val navController = rememberSwipeDismissableNavController()
            
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(dataLayerClient)
            )

            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "main_menu"
            ) {
                composable("main_menu") {
                    MainMenuScreen(
                        onNavigateToNavigation = {
                            navController.navigate("navigation")
                        }
                    )
                }
                composable("navigation") {
                    NavigationScreen(viewModel = viewModel)
                }
            }
        }
    }
}