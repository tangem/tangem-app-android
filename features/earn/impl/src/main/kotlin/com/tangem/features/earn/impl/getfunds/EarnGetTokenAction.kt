package com.tangem.features.earn.impl.getfunds

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_down_20
import com.tangem.core.ui.res.generated.icons.ic_card_20
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.features.earn.impl.R

internal enum class EarnGetTokenAction(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
) {
    Deposit(
        icon = Icons.ic_arrow_down_20,
        title = R.string.earn_get_token_deposit_title,
        subtitle = R.string.earn_get_token_deposit_subtitle,
    ),
    Swap(
        icon = Icons.ic_arrow_up_down_20,
        title = R.string.earn_get_token_swap_title,
        subtitle = R.string.earn_get_token_swap_subtitle,
    ),
    Buy(
        icon = Icons.ic_card_20,
        title = R.string.earn_get_token_buy_title,
        subtitle = R.string.earn_get_token_buy_subtitle,
    ),
    ;

    /** `null` when the wallet's action list has no such action. */
    fun reasonIn(actions: List<ActionState>): ScenarioUnavailabilityReason? = actions
        .firstOrNull { it.isOf(this) }
        ?.unavailabilityReason

    private fun ActionState.isOf(action: EarnGetTokenAction): Boolean = when (action) {
        Deposit -> this is ActionState.Receive
        Swap -> this is ActionState.Swap
        Buy -> this is ActionState.Buy
    }
}