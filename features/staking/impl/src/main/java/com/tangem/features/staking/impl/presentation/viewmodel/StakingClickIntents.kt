package com.tangem.features.staking.impl.presentation.viewmodel

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.InfoType
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

@Suppress("TooManyFunctions")
internal interface StakingClickIntents : AmountScreenClickIntents {

    fun onBackClick()

    fun onNextClick(
        actionTypeToOverwrite: StakingActionCommonType? = null,
        pendingAction: PendingAction? = null,
        pendingActions: ImmutableList<PendingAction>? = null,
        balanceState: BalanceState? = null,
    )

    fun onActionClick()

    fun onPrevClick()

    fun onRefreshSwipe(isRefreshing: Boolean)

    fun onInitialInfoBannerClick()

    fun onInfoClick(infoType: InfoType)

    fun onEnterClick()

    fun getFee(pendingAction: PendingAction?, pendingActions: ImmutableList<PendingAction>?)

    override fun onAmountNext() = onNextClick(actionTypeToOverwrite = null)

    fun openValidators()

    fun onValidatorSelect(validator: Yield.Validator)

    fun openRewardsValidators()

    fun onActiveStake(activeStake: BalanceState)

    fun onActiveStakeAnalytic()

    fun showApprovalBottomSheet()

    fun onApproveTypeChange(approveType: ApproveType)

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
