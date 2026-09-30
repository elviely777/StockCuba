package cu.stockcuba.app.domain.usecase

import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.repository.VentaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ObtenerResumenDelDiaUseCase @Inject constructor(
    private val ventaRepository: VentaRepository
) {

    suspend operator fun invoke(fecha: Long = System.currentTimeMillis()): Result<VentaRepository.ResumenDia> {
        return try {
            val resumenReal = ventaRepository.getResumenDelDia(fecha)
            if (resumenReal is Result.Failure) {
                return resumenReal
            }
            
            // Get blended total for this day (real + manual)
            val inicioDia = java.time.Instant.ofEpochMilli(fecha)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val finDia = inicioDia + 24 * 60 * 60 * 1000 - 1
            
            val blendedTotals = ventaRepository.getDailyTotalsBlended(inicioDia, finDia).first()
            val totalCombinado = blendedTotals[inicioDia] ?: (resumenReal as Result.Success<VentaRepository.ResumenDia>).value.totalVendido
            
            // Return blended resumen
            Result.Success((resumenReal as Result.Success<VentaRepository.ResumenDia>).value.copy(totalVendido = totalCombinado))
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }
}