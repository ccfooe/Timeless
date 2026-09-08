package com.classbell.app

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classbell.app.ui.navigation.ClassBellNavHost
import com.classbell.app.ui.theme.ClassBellTheme
import java.util.Locale

class LocalizedActivityContext(
    base: ComponentActivity,
    config: Configuration
) : ContextWrapper(base) {
    private val configContext: Context = base.createConfigurationContext(config)
    override fun getResources(): Resources = configContext.resources
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+) / ColorOS 16
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val app = applicationContext as ClassBellApp

        setContent {
            val appLanguage by app.settingsRepository.appLanguageFlow.collectAsStateWithLifecycle(initialValue = "zh")

            val (localizedContext, localizedConfiguration) = remember(appLanguage) {
                val locale = if (appLanguage == "en") Locale.ENGLISH else Locale.SIMPLIFIED_CHINESE
                Locale.setDefault(locale)
                val config = Configuration(resources.configuration).apply {
                    setLocale(locale)
                }
                val ctx = LocalizedActivityContext(this@MainActivity, config)
                Pair(ctx, config)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                ClassBellTheme {
                    ClassBellNavHost()
                }
            }
        }
    }
}
