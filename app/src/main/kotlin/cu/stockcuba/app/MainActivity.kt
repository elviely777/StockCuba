package cu.stockcuba.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cu.stockcuba.app.presentation.main.BloqueoScreen
import cu.stockcuba.app.presentation.main.MainViewModel
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

        // Habilitar Modo Inmersivo (Ocultar barras del sistema) (T68)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            TemaProvider {
                StockCubaTheme {
                    val viewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    val estadoNegocio by viewModel.estadoNegocio.collectAsState()
                    val isVinculado by viewModel.isVinculado.collectAsState()

                    if (isVinculado && estadoNegocio != "ACTIVO") {
                        BloqueoScreen(
                            mensaje = "Servicio Suspendido",
                            detalle = "Este negocio se encuentra en estado: $estadoNegocio. El acceso ha sido restringido por falta de pago o incumplimiento de términos."
                        )
                    } else {
                        AppNavHost()
                    }
                }
            }
        }
    }
}
