package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.*
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

internal class TangemPayFreezeUnfreezeStateTransformer(
    private val frozen: Boolean,
    private val onFreezeClick: () -> Unit,
    private val onUnfreezeClick: () -> Unit,
) : Transformer<TangemPayDetailsUM> {

    private val cardFrozenState = if (frozen) CardFrozenState.Frozen(onUnfreezeClick) else CardFrozenState.Unfrozen
    private val removeType =
        if (frozen) TangemPayDetailsTopBarMenuItemType.FreezeCard else TangemPayDetailsTopBarMenuItemType.UnfreezeCard
    private val addType =
        if (frozen) TangemPayDetailsTopBarMenuItemType.UnfreezeCard else TangemPayDetailsTopBarMenuItemType.FreezeCard

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val dropdownMenuItems = createUpdatedMenuItems(prevState.topBarConfig.items)
        val newBalanceBlockState = createUpdatedBalanceState(prevState.balanceBlockState)

        return prevState.copy(
            topBarConfig = prevState.topBarConfig.copy(items = dropdownMenuItems),
            balanceBlockState = newBalanceBlockState,
        )
    }

    private fun createUpdatedMenuItems(
        items: ImmutableList<TangemPayDetailsTopBarMenuItem>?,
    ): ImmutableList<TangemPayDetailsTopBarMenuItem>? {
        return items
            ?.filter { it.type != removeType }
            ?.plus(createMenuItemToAdd())
            ?.toPersistentList()
    }

    private fun createMenuItemToAdd(): TangemPayDetailsTopBarMenuItem {
        return TangemPayDetailsTopBarMenuItem(
            type = addType,
            dropdownItem = TangemDropdownMenuItem(
                title = resourceReference(
                    id = if (frozen) {
                        R.string.tangempay_card_details_unfreeze_card
                    } else {
                        R.string.tangempay_card_details_freeze_card
                    },
                ),
                textColor = themedColor { TangemTheme.colors.text.primary1 },
                onClick = if (frozen) onUnfreezeClick else onFreezeClick,
            ),
        )
    }

    private fun createUpdatedBalanceState(
        balanceState: TangemPayDetailsBalanceBlockState,
    ): TangemPayDetailsBalanceBlockState {
        return when (balanceState) {
            is TangemPayDetailsBalanceBlockState.Error -> balanceState.copy(frozenState = cardFrozenState)
            is TangemPayDetailsBalanceBlockState.Loading -> balanceState.copy(frozenState = cardFrozenState)
            is TangemPayDetailsBalanceBlockState.Content -> balanceState.copy(
                isBalanceFlickering = false,
                frozenState = cardFrozenState,
            )
        }
    }
}