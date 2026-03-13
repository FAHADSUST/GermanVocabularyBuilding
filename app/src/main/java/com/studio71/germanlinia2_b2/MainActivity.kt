package com.studio71.germanlinia2_b2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.studio71.germanlinia2_b2.ui.nav.AppNav
import com.studio71.germanlinia2_b2.ui.theme.GermanVocabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as GermanApp).repository
        setContent {
            GermanVocabTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(repository = repository)
                }
            }
        }
    }
}

