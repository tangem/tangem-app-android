package com.tangem.features.staking.impl.presentation.state.previewdata

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType.Coin
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.*
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal object ConfirmationStatePreviewData {

    private val fee = Fee.Common(
        amount = Amount(
            currencySymbol = "MATIC",
            value = BigDecimal(0.159806),
            decimals = 18,
            type = Coin,
        ),
    )

    val assentStakingState = StakingStates.ConfirmationState.Data(
        isPrimaryButtonEnabled = true,
        innerState = InnerConfirmationStakingState.ASSENT,
        feeState = FeeState.Content(
            fee = fee,
            rate = BigDecimal.ONE,
            appCurrency = AppCurrency.Default,
            isFeeApproximate = false,
            isFeeConvertibleToFiat = true,
        ),
        footerText = stringReference("You stake \$715.11 and will be receiving ~\$35 monthly"),
        notifications = persistentListOf(
            StakingNotification.Info.EarnRewards(
                subtitleText = resourceReference(
                    id = R.string.staking_notification_earn_rewards_text_period_day,
                    formatArgs = wrappedList("Solana"),
                ),
            ),
        ),
        transactionDoneState = TransactionDoneState.Empty,
        pendingAction = null,
        isApprovalNeeded = false,
        allowance = BigDecimal.ZERO,
        reduceAmountBy = null,
        pendingActions = null,
        isAmountEditable = true,
    )
}