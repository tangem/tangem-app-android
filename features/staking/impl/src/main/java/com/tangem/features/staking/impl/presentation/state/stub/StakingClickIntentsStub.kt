package com.tangem.features.staking.impl.presentation.state.stub

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.InfoType
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

@Suppress("TooManyFunctions")
internal object StakingClickIntentsStub : StakingClickIntents {

    override fun onBackClick() {}

    override fun onNextClick(
        actionTypeToOverwrite: StakingActionCommonType?,
        pendingAction: PendingAction?,
        pendingActions: ImmutableList<PendingAction>?,
        balanceState: BalanceState?,
    ) { }

    override fun onActionClick() {}

    override fun onPrevClick() {}

    override fun onRefreshSwipe(isRefreshing: Boolean) {}

    override fun onInitialInfoBannerClick() {}

    override fun onInfoClick(infoType: InfoType) {}

    override fun onEnterClick() {}

    override fun onAmountValueChange(value: String) {}

    override fun onAmountPasteTriggerDismiss() {}

    override fun onMaxValueClick() {}

    override fun onCurrencyChangeClick(isFiat: Boolean) {}

    override fun onAmountNext() {}

    override fun openValidators() {}

    override fun onValidatorSelect(validator: Yield.Validator) {}

    override fun openRewardsValidators() {}

    override fun showApprovalBottomSheet() {}

    override fun onApprovalClick() {}

    override fun onExploreClick() {}

    override fun onShareClick() {}

    override fun onFailedTxEmailClick(errorMessage: String) {}

    override fun onActiveStake(activeStake: BalanceState) {}

    override fun getFee(pendingAction: PendingAction?, pendingActions: ImmutableList<PendingAction>?) {
    }

    override fun onAmountReduceByClick(
        reduceAmountBy: BigDecimal,
        reduceAmountByDiff: BigDecimal,
        notification: Class<out NotificationUM>,
    ) {
    }

    override fun onAmountReduceToClick(reduceAmountTo: BigDecimal, notification: Class<out NotificationUM>) {}

    override fun onNotificationCancel(notification: Class<out NotificationUM>) {}

    override fun openTokenDetails(cryptoCurrency: CryptoCurrency) {}

    override fun onActiveStakeAnalytic() {}
}
