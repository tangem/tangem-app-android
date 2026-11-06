package com.tangem.features.earn.impl.picker.model

import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.earn.impl.guardedBy
import com.tangem.features.earn.impl.picker.EarnPickerTexts
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.session.EarnAction
import java.math.BigDecimal
import javax.inject.Inject

/** The bottom button of the picker: one rule table, the first matching row wins. */
internal class EarnPickerCta @Inject constructor() {

    fun resolve(
        state: EarnProductState,
        ethBalance: BigDecimal?,
        onActionClick: (EarnAction) -> Unit,
        onBlocked: (EarnBlock) -> Unit,
        onAddFunds: () -> Unit,
    ): Result {
        if (state !is EarnProductState.Ready) {
            return Result(button = button(EarnPickerTexts.startEarning, isEnabled = false), hint = null)
        }

        val primary = state.primary
        if (ethBalance == null) {
            return Result(button = button(primary.label, isEnabled = false), hint = null)
        }

        val block = primary.guardedBy(state).block
        val minDeposit = state.limits.minDeposit
        return when {
            block != null -> Result(
                button = button(primary.label, isDimmed = true, onClick = { onBlocked(block) }),
                hint = null,
            )
            ethBalance < minDeposit -> Result(
                button = button(EarnPickerTexts.addFunds, onClick = onAddFunds),
                hint = EarnPickerTexts.minDepositHint(minDeposit),
            )
            else -> Result(
                button = button(primary.label, onClick = { onActionClick(primary.action) }),
                hint = null,
            )
        }
    }

    data class Result(val button: TangemButtonUM, val hint: TextReference?)

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