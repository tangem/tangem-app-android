package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayDetailsTopBarMenuItem
import com.tangem.features.tangempay.entity.TangemPayDetailsTopBarMenuItemType.FreezeCard
import com.tangem.features.tangempay.entity.TangemPayDetailsTopBarMenuItemType.UnfreezeCard
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

internal class TangemPayFreezeUnfreezeStateTransformer(
    private val cardFrozenState: TangemPayCardFrozenState,
    private val onFreezeClick: () -> Unit,
    private val onUnfreezeClick: () -> Unit,
    private val converter: TangemPayCardFrozenStateConverter,
) : Transformer<TangemPayDetailsUM> {

    private val isCardFrozen: Boolean = when (cardFrozenState) {
        is TangemPayCardFrozenState.Frozen -> true
        is TangemPayCardFrozenState.Unfrozen, is TangemPayCardFrozenState.Pending -> false
    }

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val dropdownMenuItems = when (cardFrozenState) {
            TangemPayCardFrozenState.Frozen,
            TangemPayCardFrozenState.Unfrozen,
            -> createUpdatedMenuItems(prevState.topBarConfig.items)
            TangemPayCardFrozenState.Pending -> prevState.topBarConfig.items
        }

        return prevState.copy(
            topBarConfig = prevState.topBarConfig.copy(items = dropdownMenuItems),
            cardFrozenState = converter.convert(cardFrozenState),
        )
    }

    private fun createUpdatedMenuItems(
        items: ImmutableList<TangemPayDetailsTopBarMenuItem>?,
    ): ImmutableList<TangemPayDetailsTopBarMenuItem>? {
        return items
            ?.filterNot { it.type == UnfreezeCard || it.type == FreezeCard }
            ?.plus(createMenuItemToAdd())
            ?.toPersistentList()
    }

    private fun createMenuItemToAdd(): TangemPayDetailsTopBarMenuItem {
        return TangemPayDetailsTopBarMenuItem(
            type = if (isCardFrozen) UnfreezeCard else FreezeCard,
            dropdownItem = TangemDropdownMenuItem(
                title = resourceReference(
                    id = if (isCardFrozen) {
                        R.string.tangempay_card_details_unfreeze_card
                    } else {
                        R.string.tangempay_card_details_freeze_card
                    },
                ),
                textColor = themedColor { TangemTheme.colors.text.primary1 },
                onClick = if (isCardFrozen) onUnfreezeClick else onFreezeClick,
            ),
        )
    }
}