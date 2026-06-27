package com.farsh.carpetmapreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.farsh.carpetmapreader.data.AppDatabase
import com.farsh.carpetmapreader.data.MapRepository
import com.farsh.carpetmapreader.ui.CarpetApp
import com.farsh.carpetmapreader.ui.CarpetViewModel
import com.farsh.carpetmapreader.ui.CarpetViewModelFactory
import com.farsh.carpetmapreader.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable modern Edge-to-Edge full bleed layout support
        enableEdgeToEdge()
        
        // Setup local SQLite Room persistence engines
        val database = AppDatabase.getDatabase(this)
        val repository = MapRepository(database.mapDao())
        
        // Build the state-managed CarpetViewModel
        val viewModelFactory = CarpetViewModelFactory(applicationContext, repository)
        val carpetViewModel: CarpetViewModel by viewModels { viewModelFactory }

        setContent {
            MyApplicationTheme {
                CarpetApp(viewModel = carpetViewModel)
            }
        }
    }
}
