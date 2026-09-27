package com.foodpack.downtime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.foodpack.downtime.data.DowntimeRepository
import com.foodpack.downtime.sync.SyncScheduler
import com.foodpack.downtime.ui.DowntimeApp
import com.foodpack.downtime.ui.theme.DowntimeTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SyncScheduler.schedulePeriodic(applicationContext)
        val repository = DowntimeRepository(applicationContext)

        setContent {
            DowntimeTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DowntimeApp(repository = repository)
                }
            }
        }
    }
}
