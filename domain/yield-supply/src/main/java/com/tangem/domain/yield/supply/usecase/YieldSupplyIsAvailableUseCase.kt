package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.isJoint
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.YieldSupplyWrappedNativeFactory
import com.tangem.domain.yield.supply.isWrappedNative

class YieldSupplyIsAvailableUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
    private val wrappedNativeFactory: YieldSupplyWrappedNativeFactory,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        if (cryptoCurrency.isJoint) return false

        if (tokensFeatureToggles.isYieldContributionEnabled && wrappedNativeFactory.isWrappedNative(cryptoCurrency)) {
            return false
        }

        return yieldSupplyRepository.isYieldSupplySupported(userWalletId, cryptoCurrency)
    }
}