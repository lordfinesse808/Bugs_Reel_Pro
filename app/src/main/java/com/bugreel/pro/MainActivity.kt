package com.bugreel.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bugreel.pro.navigation.AppRoot
import dagger.hilt.android.AndroidEntryPoint

// CLAUDE.md Section 14 item 49 — single-activity host; all screens are Compose destinations
// reached through AppNavGraph, never separate Activities. AppRoot (Section 18) gates entry to
// AppNavGraph behind consent.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}
