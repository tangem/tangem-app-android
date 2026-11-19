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

    fun kind(state: EarnProductState, ethBalance: BigDecimal?): Kind = when {
        state !is EarnProductState.Ready || ethBalance == null -> Kind.Disabled
        blockOf(state) != null -> Kind.Blocked
        state.position == EarnPositionState.None && isBelowDeposit(state, ethBalance) -> Kind.AddFunds
        else -> Kind.Primary
    }

    fun resolve(
        state: EarnProductState,
        ethBalance: BigDecimal?,
        onActionClick: (EarnAction) -> Unit,
        onBlocked: (EarnBlock) -> Unit,
        onAddFunds: () -> Unit,
    ): TangemButtonUM {
        if (state !is EarnProductState.Ready) {
            return button(resourceReference(R.string.eth_yield_start_earning), isEnabled = false)
        }

        val primary = state.primary
        return when (kind(state, ethBalance)) {
            Kind.Disabled -> button(primary.label, isEnabled = false)
            Kind.Blocked -> button(primary.label, isDimmed = true, onClick = { blockOf(state)?.let(onBlocked) })
            Kind.AddFunds -> button(resourceReference(R.string.common_add_funds), onClick = onAddFunds)
            Kind.Primary -> button(primary.label, onClick = { onActionClick(primary.action) })
        }
    }

    enum class Kind { Disabled, Blocked, AddFunds, Primary }

    private fun blockOf(state: EarnProductState.Ready): EarnBlock? = state.primary.guardedBy(state).block

    // A product without a minimum deposit still has nothing to deposit from an empty balance
    private fun isBelowDeposit(state: EarnProductState.Ready, ethBalance: BigDecimal): Boolean =
        ethBalance.signum() == 0 || ethBalance < state.limits.minDeposit

    private fun button(
        text: TextReference,
        isEnabled: Boolean = true,
        isDimmed: Boolean = false,
        onClick: () -> Unit = {},
    ) = TangemButtonUM(
        text = text,
        isEnabled = isEnabled,
        isDimmed = isDimmed,
        type = TangemButtonType.Primary,
        onClick = onClick,
    )
}