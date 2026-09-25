package com.sidequests.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.sidequests.app.analytics.SupabaseContextAnalytics
import com.sidequests.app.context.AndroidLocationProvider
import com.sidequests.app.context.ContextManager
import com.sidequests.app.context.OpenMeteoWeatherService
import com.sidequests.app.ui.AppViewModel
import com.sidequests.app.ui.AppViewModelFactory
import com.sidequests.app.ui.SidequestsApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val contextManager = ContextManager(
            locationProvider = AndroidLocationProvider(applicationContext),
            weatherService = OpenMeteoWeatherService(),
        )

        val factory = AppViewModelFactory(
            contextManager = contextManager,
            analytics = SupabaseContextAnalytics(),
        )

        val appViewModel = ViewModelProvider(this, factory)[AppViewModel::class.java]

        setContent {
            SidequestsApp(appViewModel = appViewModel)
        }
    }
}
