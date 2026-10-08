package ru.servermonitor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.slf4j.LoggerFactory
import ru.servermonitor.app.data.model.Server
import ru.servermonitor.app.data.model.ServerEnvironment
import ru.servermonitor.app.data.storage.ServerFileStorage
import ru.servermonitor.app.ui.theme.ServerMonitorTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private val logger = LoggerFactory.getLogger(MainActivity::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        logger.info("Application started")

        val storage = ServerFileStorage(File(filesDir, ServerFileStorage.FILE_NAME))
        storage.load()
        storage.add(
            Server(
                id = DEMO_SERVER_ID,
                name = "Учебный сервер",
                host = "127.0.0.1",
                port = 8080,
                environment = ServerEnvironment.DEVELOPMENT,
            ),
        )
        storage.save()
        storage.load()
        logger.info("storage check: {} servers restored", storage.servers.size)

        enableEdgeToEdge()
        setContent {
            ServerMonitorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ServerMonitorTheme {
        Greeting("Android")
    }
}

private const val DEMO_SERVER_ID = "00000000-0000-0000-0000-000000000003"
