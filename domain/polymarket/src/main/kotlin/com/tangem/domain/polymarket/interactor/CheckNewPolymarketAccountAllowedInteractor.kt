package com.tangem.domain.polymarket.interactor

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketNewAccountAllowance
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
import com.tangem.utils.logging.TangemLogger

class CheckNewPolymarketAccountAllowedInteractor(
    private val getWalletStatus: GetPolymarketWalletStatusUseCase,
    private val checkGeoblock: CheckPolymarketGeoblockUseCase,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): PolymarketNewAccountAllowance {
        val status = getWalletStatus.byWalletId(userWalletId)
            .onLeft { error -> TangemLogger.i("Region gate: wallet status unavailable, asking the region: $error") }
            .getOrNull()
            ?.status

        if (status != null && !status.needsDeploy()) return PolymarketNewAccountAllowance.ALLOWED

        return checkGeoblock().fold(
            ifLeft = { error ->
                TangemLogger.e("Region gate: region check failed, new account refused: $error")
                PolymarketNewAccountAllowance.UNDETERMINED
            },
            ifRight = { isBlocked ->
                if (isBlocked) {
                    PolymarketNewAccountAllowance.REGION_BLOCKED
                } else {
                    PolymarketNewAccountAllowance.ALLOWED
                }
            },
        )
    }
}