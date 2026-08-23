package cu.stockcuba.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import cu.stockcuba.app.presentation.navigation.AppNavHost
import cu.stockcuba.app.presentation.theme.StockCubaTheme
import cu.stockcuba.app.presentation.theme.TemaProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TemaProvider {
                StockCubaTheme {
                    AppNavHost()
                }
            }
        }
    }
}