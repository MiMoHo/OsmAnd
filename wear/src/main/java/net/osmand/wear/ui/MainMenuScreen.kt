package net.osmand.wear.ui

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text

@Composable
fun MainMenuScreen(
    onNavigateToNavigation: () -> Unit
) {
    val context = LocalContext.current

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            ListHeader {
                Text("OsmAnd")
            }
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToNavigation,
                label = { Text("Navigation") },
                icon = {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_directions),
                        contentDescription = "Navigation Icon"
                    )
                },
                colors = ButtonDefaults.buttonColors()
            )
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    Toast.makeText(context, "Coming Soon", Toast.LENGTH_SHORT).show()
                },
                label = { Text("Trip recording") },
                icon = {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                        contentDescription = "Trip Recording Icon"
                    )
                },
                colors = ButtonDefaults.buttonColors() // or secondaryButtonColors
            )
        }
    }
}
