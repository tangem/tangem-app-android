package com.tangem.features.staking.impl.presentation.viewmodel

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.transformers.InfoType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal interface StakingClickIntents : AmountScreenClickIntents {

    fun onBackClick()

    fun onNextClick(
        actionType: StakingActionCommonType? = null,
        pendingActions: ImmutableList<PendingAction> = persistentListOf(),
    )

    fun onActionClick(pendingAction: PendingAction?)

    fun onPrevClick()

    fun onInitialInfoBannerClick()

    fun onInfoClick(infoType: InfoType)

    override fun onAmountNext() = onNextClick(actionType = null)

    fun openValidators()

    fun onValidatorSelect(validator: Yield.Validator)

    fun openRewardsValidators()

    fun onActiveStake(activeStake: BalanceState)

    fun showApprovalBottomSheet()

    fun onApprovalClick()

    fun onExploreClick()

    fun onShareClick()
}