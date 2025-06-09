package com.tangem.features.staking.impl.presentation.model

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.InfoType
import java.math.BigDecimal

@Suppress("TooManyFunctions")
internal interface StakingClickIntents : AmountScreenClickIntents {

    fun onBackClick()

    fun onNextClick(balanceState: BalanceState? = null)

    fun onActionClick()

    fun onPrevClick()

    fun onRefreshSwipe(isRefreshing: Boolean)

    fun onInitialInfoBannerClick()

    fun onInfoClick(infoType: InfoType)

    fun onAmountEnterClick()

    fun getFee()

    override fun onAmountNext() = onNextClick()

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

    fun showPrimaryClickAlert()
}