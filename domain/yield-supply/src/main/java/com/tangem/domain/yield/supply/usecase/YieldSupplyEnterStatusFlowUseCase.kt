package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus
import kotlinx.coroutines.flow.Flow

class YieldSupplyEnterStatusFlowUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    operator fun invoke(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Flow<YieldSupplyPendingStatus?> {
        return yieldSupplyRepository.getTokenProtocolPendingStatusFlow(userWalletId, cryptoCurrency)
    }
}