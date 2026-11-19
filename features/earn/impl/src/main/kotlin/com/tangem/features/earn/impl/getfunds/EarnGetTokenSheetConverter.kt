package com.tangem.features.earn.impl.getfunds

import com.tangem.common.ui.tokens.rendersActionAsEnabled
import com.tangem.common.ui.tokens.rendersSwapAsEnabled
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.domain.tokens.model.isLoading
import com.tangem.features.earn.impl.R
import kotlinx.collections.immutable.toImmutableList

internal object EarnGetTokenSheetConverter {

    /** [isReceiveCached]: the receive address of a cached status can be stale, so Deposit waits for the network. */
    fun convert(
        currency: CryptoCurrency,
        actions: List<ActionState>,
        isReceiveCached: Boolean,
        onActionClick: (EarnGetTokenAction) -> Unit,
        onDismiss: () -> Unit,
    ): EarnGetTokenSheetUM {
        val symbol = wrappedList<Any>(currency.symbol)

        return EarnGetTokenSheetUM(
            title = resourceReference(R.string.get_token_title, symbol),
            rows = EarnGetTokenAction.entries.mapNotNull { action ->
                val reason = action.reasonIn(actions) ?: return@mapNotNull null
                EarnGetTokenSheetUM.Row(
                    icon = action.icon,
                    title = resourceReference(action.title, symbol),
                    subtitle = resourceReference(action.subtitle),
                    isLoading = reason.isLoading || action == EarnGetTokenAction.Deposit && isReceiveCached,
                    isEnabled = if (action == EarnGetTokenAction.Swap) {
                        reason.rendersSwapAsEnabled()
                    } else {
                        reason.rendersActionAsEnabled()
                    },
                    onClick = { onActionClick(action) },
                )
            }.toImmutableList(),
            onDismiss = onDismiss,
        )
    }
}