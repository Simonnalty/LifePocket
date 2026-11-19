package com.simon.lifepocket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.simon.lifepocket.ui.Root
import com.simon.lifepocket.ui.theme.LifePocketTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifePocketTheme {
                Root()
            }
        }
    }
}
