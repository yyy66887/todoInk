package com.todoink.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.todoink.app.ui.TodoInkApp
import com.todoink.app.ui.theme.TodoInkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodoInkTheme {
                TodoInkApp()
            }
        }
    }
}
