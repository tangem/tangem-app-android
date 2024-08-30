package com.tangem.features.staking.impl.presentation.viewmodel

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.InfoType
import java.math.BigDecimal

@Suppress("TooManyFunctions")
internal interface StakingClickIntents : AmountScreenClickIntents {

    fun onBackClick()

    fun onNextClick(actionType: StakingActionCommonType? = null, pendingAction: PendingAction? = null)

    fun onActionClick()

    fun onPrevClick()

    fun onInitialInfoBannerClick()

    fun onInfoClick(infoType: InfoType)

    fun getFee(pendingAction: PendingAction?)

    override fun onAmountNext() = onNextClick(actionType = null)

    fun openValidators()

    fun onValidatorSelect(validator: Yield.Validator)

    fun openRewardsValidators()

    fun onActiveStake(activeStake: BalanceState)

    fun showApprovalBottomSheet()

    fun onApprovalClick()

    fun onAmountReduceByClick(
        reduceAmountBy: BigDecimal,
        reduceAmountByDiff: BigDecimal,
        notification: Class<out NotificationUM>,
    )

    fun onAmountReduceToClick(reduceAmountTo: BigDecimal, notification: Class<out NotificationUM>)

    fun onNotificationCancel(notification: Class<out NotificationUM>)

    fun onExploreClick()

    fun onShareClick()

    fun onFailedTxEmailClick(errorMessage: String)

    fun openTokenDetails(cryptoCurrency: CryptoCurrency)
}
