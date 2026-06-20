package com.kargathra.fitness.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kargathra.fitness.R
import com.kargathra.fitness.ui.theme.KargathraTheme

/**
 * Shown by Health Connect when the user taps "see why this app needs access".
 * Required for the integration to be considered complete.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KargathraTheme { RationaleScreen() } }
    }
}

@Composable
private fun RationaleScreen() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Health Connect & Kargathra", style = MaterialTheme.typography.headlineSmall)
            Text(
                stringRes(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun stringRes(): String =
    androidx.compose.ui.platform.LocalContext.current.getString(R.string.health_permissions_rationale)
