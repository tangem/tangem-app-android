package com.tangem.features.earn.impl.picker.model

import com.tangem.core.res.R
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.earn.impl.guardedBy
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnPositionState
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.session.EarnAction
import java.math.BigDecimal
import javax.inject.Inject

internal class EarnPickerCta @Inject constructor() {

    fun kind(state: EarnProductState, ethBalance: BigDecimal?, capacity: EarnPickerDepositCapacity): Kind = when {
        state !is EarnProductState.Ready || ethBalance == null -> Kind.Disabled
        blockOf(state) != null -> Kind.Blocked
        state.position != EarnPositionState.None -> Kind.Primary
        isBelowDeposit(state, ethBalance) || isFeeUncovered(state, capacity) -> Kind.AddFunds
        capacity == EarnPickerDepositCapacity.Loading -> Kind.Loading
        else -> Kind.Primary
    }

    @Suppress("LongParameterList")
    fun resolve(
        state: EarnProductState,
        ethBalance: BigDecimal?,
        capacity: EarnPickerDepositCapacity,
        onActionClick: (EarnAction) -> Unit,
        onBlocked: (EarnBlock) -> Unit,
        onAddFunds: () -> Unit,
    ): TangemButtonUM {
        if (state !is EarnProductState.Ready) {
            return button(resourceReference(R.string.eth_yield_start_earning), isEnabled = false)
        }

        val primary = state.primary
        return when (kind(state, ethBalance, capacity)) {
            Kind.Disabled -> button(primary.label, isEnabled = false)
            Kind.Blocked -> button(primary.label, isDimmed = true, onClick = { blockOf(state)?.let(onBlocked) })
            Kind.AddFunds -> button(resourceReference(R.string.common_add_funds), onClick = onAddFunds)
            Kind.Loading -> button(primary.label, isEnabled = false, isLoading = true)
            Kind.Primary -> button(primary.label, onClick = { onActionClick(primary.action) })
        }
    }

    enum class Kind { Disabled, Blocked, AddFunds, Loading, Primary }

    private fun blockOf(state: EarnProductState.Ready): EarnBlock? = state.primary.guardedBy(state).block

    // A product without a minimum deposit still has nothing to deposit from an empty balance
    private fun isBelowDeposit(state: EarnProductState.Ready, ethBalance: BigDecimal): Boolean =
        ethBalance.signum() == 0 || ethBalance < state.limits.minDeposit

    private fun isFeeUncovered(state: EarnProductState.Ready, capacity: EarnPickerDepositCapacity): Boolean =
        capacity is EarnPickerDepositCapacity.Known &&
            (capacity.availableMax.signum() <= 0 || capacity.availableMax < state.limits.minDeposit)

    private fun button(
        text: TextReference,
        isEnabled: Boolean = true,
        isDimmed: Boolean = false,
        isLoading: Boolean = false,
        onClick: () -> Unit = {},
    ) = TangemButtonUM(
        text = text,
        isEnabled = isEnabled,
        isDimmed = isDimmed,
        isLoading = isLoading,
        type = TangemButtonType.Primary,
        onClick = onClick,
    )
}