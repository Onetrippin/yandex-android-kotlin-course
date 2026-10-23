package ru.servermonitor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.slf4j.LoggerFactory
import ru.servermonitor.app.ui.editor.ServerEditorRoute
import ru.servermonitor.app.ui.theme.ServerMonitorTheme

class MainActivity : ComponentActivity() {
    private val logger = LoggerFactory.getLogger(MainActivity::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        logger.info("Application started")
        enableEdgeToEdge()
        setContent {
            ServerMonitorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ServerEditorRoute()
                }
            }
        }
    }
}
