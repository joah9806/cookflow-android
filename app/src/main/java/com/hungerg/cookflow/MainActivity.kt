package com.hungerg.cookflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hungerg.cookflow.ui.CookFlowApp
import com.hungerg.cookflow.ui.theme.CookFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CookFlowTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CookFlowApp(viewModel = viewModel())
                }
            }
        }
    }
}
