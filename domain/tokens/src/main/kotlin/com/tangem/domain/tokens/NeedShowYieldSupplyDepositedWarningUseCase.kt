package com.tangem.domain.tokens

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.tokens.repository.YieldSupplyWarningsViewedRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

@Suppress("UnusedPrivateProperty")
class NeedShowYieldSupplyDepositedWarningUseCase(
    private val yieldSupplyWarningsViewedRepository: YieldSupplyWarningsViewedRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(cryptoCurrencyStatus: CryptoCurrencyStatus?): Boolean = withContext(dispatchers.io) {
        // TEMPORARY REQUIREMENTS
        return@withContext false
        // val hasActiveLending = cryptoCurrencyStatus?.value?.yieldSupplyStatus?.isActive == true
        // if (!hasActiveLending) return@withContext false
        // val showedWarnings = yieldSupplyWarningsViewedRepository.getViewedWarnings()
        // return@withContext !showedWarnings.contains(cryptoCurrencyStatus?.currency?.name)
    }
}