package com.tangem.features.staking.impl.presentation.state.stub

import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.presentation.model.StakingClickIntents
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.InfoType
import java.math.BigDecimal

@Suppress("TooManyFunctions")
internal object StakingClickIntentsStub : StakingClickIntents {

    override fun onBackClick() {}

    override fun onNextClick(balanceState: BalanceState?) {}

    override fun onActionClick() {}

    override fun onPrevClick() {}

    override fun onRefreshSwipe(isRefreshing: Boolean) {}

    override fun onInitialInfoBannerClick() {}

    override fun onInfoClick(infoType: InfoType) {}

    override fun onAmountEnterClick() {}

    override fun onAmountValueChange(value: String) {}

    override fun onAmountPasteTriggerDismiss() {}

    override fun onMaxValueClick() {}

    override fun onCurrencyChangeClick(isFiat: Boolean) {}

    override fun onAmountNext() {}

    override fun openValidators() {}

    override fun onValidatorSelect(validator: Yield.Validator) {}

    override fun openRewardsValidators() {}

    override fun showApprovalBottomSheet() {}

    override fun onApproveTypeChange(approveType: ApproveType) {}

    override fun onApprovalClick() {}

    override fun onExploreClick() {}

    override fun onShareClick() {}

    override fun onFailedTxEmailClick(errorMessage: String) {}

    override fun onActiveStake(activeStake: BalanceState) {}

    override fun getFee() {}

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

    override fun showPrimaryClickAlert() {}
}