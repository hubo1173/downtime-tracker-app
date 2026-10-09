package com.foodpack.downtime

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.foodpack.downtime.reminders.ReminderScheduler
import kotlinx.coroutines.launch
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

        // Android 13+ needs the user's OK before reminders can show.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        lifecycleScope.launch { ReminderScheduler.scheduleAll(applicationContext) }
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
