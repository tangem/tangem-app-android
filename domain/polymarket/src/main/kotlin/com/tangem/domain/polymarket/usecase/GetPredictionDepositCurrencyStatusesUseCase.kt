package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.PredictionDepositRequisitesStore
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class GetPredictionDepositCurrencyStatusesUseCase(
    private val requisitesStore: PredictionDepositRequisitesStore,
    private val predictionAccountStatusSupplier: PredictionAccountStatusSupplier,
    private val collateralCurrencyFactory: PolymarketCollateralCurrencyFactory,
) {

    /**
     * @return one status per token the account can be topped up in, each at its bridge address and carrying the
     *  trading balance, the default deposit token first; empty while the account is not active or the bridge has
     *  not confirmed the requisites this session
     */
    suspend operator fun invoke(userWalletId: UserWalletId): List<CryptoCurrencyStatus> {
        val status = withTimeoutOrNull(STATUS_READ_TIMEOUT) {
            predictionAccountStatusSupplier(userWalletId).firstOrNull { it !is PredictionAccountStatusValue.Loading }
        }
        val active = status as? PredictionAccountStatusValue.Active ?: return emptyList()
        val requisites = requisitesStore.getSyncOrNull(userWalletId) ?: return emptyList()

        return requisites.options.map { option ->
            active.depositStatus(
                currency = collateralCurrencyFactory.createDepositTarget(option.asset),
                address = option.target.address,
            )
        }
    }

    private companion object {
        val STATUS_READ_TIMEOUT = 5.seconds
    }
}