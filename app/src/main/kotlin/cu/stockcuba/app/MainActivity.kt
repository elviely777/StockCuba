package cu.stockcuba.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.viewModels
import cu.stockcuba.app.presentation.navigation.AppNavHost
import cu.stockcuba.app.presentation.security.TrialExpiredScreen
import cu.stockcuba.app.presentation.security.TrialStatus
import cu.stockcuba.app.presentation.security.TrialViewModel
import cu.stockcuba.app.presentation.theme.StockCubaTheme
import cu.stockcuba.app.presentation.theme.TemaProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val trialViewModel: TrialViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Habilitar Modo Inmersivo (Ocultar barras del sistema) (T68)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            val trialStatus by trialViewModel.trialStatus.collectAsStateWithLifecycle()

            TemaProvider {
                StockCubaTheme {
                    when (trialStatus) {
                        is TrialStatus.Expired -> TrialExpiredScreen()
                        is TrialStatus.Checking -> { /* Show nothing or splash */ }
                        else -> AppNavHost()
                    }
                }
            }
        }
    }
}