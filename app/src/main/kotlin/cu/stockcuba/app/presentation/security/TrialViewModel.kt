package cu.stockcuba.app.presentation.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.presentation.ajustes.AjustesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class TrialViewModel @Inject constructor(
    private val ajustesDataStore: AjustesDataStore
) : ViewModel() {

    private val TRIAL_DAYS = 7

    val trialStatus = ajustesDataStore.fechaInstalacion.map { installDate ->
        if (installDate == null) {
            // First run, record the date
            val now = Instant.now().toEpochMilli()
            viewModelScope.launch {
                ajustesDataStore.guardarFechaInstalacion(now)
            }
            TrialStatus.Active(daysLeft = TRIAL_DAYS)
        } else {
            val installInstant = Instant.ofEpochMilli(installDate)
            val now = Instant.now()
            val daysUsed = ChronoUnit.DAYS.between(installInstant, now).toInt()
            
            if (daysUsed >= TRIAL_DAYS) {
                TrialStatus.Expired
            } else {
                TrialStatus.Active(daysLeft = TRIAL_DAYS - daysUsed)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrialStatus.Checking)
}

sealed interface TrialStatus {
    data object Checking : TrialStatus
    data class Active(val daysLeft: Int) : TrialStatus
    data object Expired : TrialStatus
}
