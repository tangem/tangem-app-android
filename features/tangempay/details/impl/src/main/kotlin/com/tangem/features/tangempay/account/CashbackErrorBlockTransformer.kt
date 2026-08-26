package com.tangem.features.tangempay.account

import com.tangem.core.ui.R
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

/**
 * Applied when the cashback summary request fails: shows the "Cashback unavailable / Tap to reload"
 * block in place of the cashback widget and removes the cashback menu entry, since without a summary
 * the display mode is unknown. Tapping the block retries the request via [onReload]; while the retry
 * is in flight the block shows a progress indicator ([isReloading]).
 */
internal class CashbackErrorBlockTransformer(
    private val onReload: () -> Unit,
    private val isReloading: Boolean,
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        return prevState.copy(
            cashbackBlockState = CashbackBlockUM.Error(onReload = onReload, isReloading = isReloading),
            topBarConfig = prevState.topBarConfig.copy(
                items = prevState.topBarConfig.items
                    .filterNot { it.isTitledWith(R.string.tangempay_cashback_menu_item_title) }
                    .toImmutableList(),
            ),
        )
    }
}