package com.tangem.features.earn.impl.picker.model

import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.earn.impl.guardedBy
import com.tangem.features.earn.impl.picker.EarnPickerTexts
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnPositionState
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.session.EarnAction
import java.math.BigDecimal
import javax.inject.Inject

internal class EarnPickerCta @Inject constructor() {

    fun resolve(
        state: EarnProductState,
        ethBalance: BigDecimal?,
        onActionClick: (EarnAction) -> Unit,
        onBlocked: (EarnBlock) -> Unit,
        onAddFunds: () -> Unit,
    ): TangemButtonUM {
        if (state !is EarnProductState.Ready) {
            return button(EarnPickerTexts.startEarning, isEnabled = false)
        }

        val primary = state.primary
        if (ethBalance == null) {
            return button(primary.label, isEnabled = false)
        }

        val block = primary.guardedBy(state).block
        val isStart = state.position == EarnPositionState.None
        // A product without a minimum deposit still has nothing to deposit from an empty balance
        val isBelowDeposit = ethBalance.signum() == 0 || ethBalance < state.limits.minDeposit
        return when {
            block != null -> button(primary.label, isDimmed = true, onClick = { onBlocked(block) })
            isStart && isBelowDeposit -> button(EarnPickerTexts.addFunds, onClick = onAddFunds)
            else -> button(primary.label, onClick = { onActionClick(primary.action) })
        }
    }

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