package com.tangem.features.staking.impl.presentation.state.helpers

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.usecase.EstimateFeeUseCase
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Returns the fee used by the rent-exemption check ([REDACTED_TASK_KEY]).
 *
 * When StakeKit provides a fee, returns it as-is. When the StakeKit fee is unavailable (`null`) —
 * which happens on Solana when the post-transaction balance would drop below the rent-exempt minimum
 * and StakeKit fails its fee calculation — falls back to a client-side Solana fee estimate so the
 * rent-exemption check can still run independently of StakeKit. Returns `null` for non-Solana
 * networks or when estimation fails (in those cases the rent check stays inert, preserving previous
 * behavior).
 */
internal class GetEffectiveStakingFee @Inject constructor(
    private val estimateFeeUseCase: EstimateFeeUseCase,
) {

    suspend operator fun invoke(
        stakeKitFee: BigDecimal?,
        amount: BigDecimal?,
        userWallet: UserWallet,
        feeCurrencyStatus: CryptoCurrencyStatus?,
    ): BigDecimal? {
        if (stakeKitFee != null) return stakeKitFee
        if (feeCurrencyStatus == null) return null
        if (!BlockchainUtils.isSolana(feeCurrencyStatus.currency.network.rawId)) return null

        return estimateFeeUseCase(
            amount = amount.orZero(),
            userWallet = userWallet,
            cryptoCurrencyStatus = feeCurrencyStatus,
        ).getOrNull()?.normal?.amount?.value
    }
}