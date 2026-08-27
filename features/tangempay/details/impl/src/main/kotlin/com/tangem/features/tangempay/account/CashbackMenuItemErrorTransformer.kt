package com.tangem.features.tangempay.account

import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.ColorReference2
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_percent_backward_20
import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
import com.tangem.utils.transformer.Transformer

/**
 * Applied when the cashback summary refresh fails while the three-dots menu entry (alt_block mode)
 * is the shown cashback UI: swaps the entry for an error-tinted "Cashback / Tap to reload" item,
 * or a disabled "Cashback / Loading..." item while the retry is in flight ([isReloading]).
 * The cashback block on the screen stays hidden, as it always is in alt_block mode.
 */
internal class CashbackMenuItemErrorTransformer(
    private val onReload: () -> Unit,
    private val isReloading: Boolean,
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        return prevState.copy(
            cashbackBlockState = null,
            topBarConfig = prevState.topBarConfig.copy(
                items = prevState.topBarConfig.items.withCashbackMenuItem(buildMenuItem()),
            ),
        )
    }

    private fun buildMenuItem(): TangemPayDropDownItemUM {
        return if (isReloading) {
            TangemPayDropDownItemUM(
                title = resourceReference(R.string.tangempay_cashback_title),
                subtitle = resourceReference(R.string.tangempay_cashback_menu_item_loading),
                onClick = {},
                isEnabled = false,
                titleColor = ColorReference2 { TangemTheme.colors3.text.secondary },
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_percent_backward_20,
                    tintReference = { TangemTheme.colors3.icon.secondary },
                ),
            )
        } else {
            TangemPayDropDownItemUM(
                title = resourceReference(R.string.tangempay_cashback_title),
                subtitle = resourceReference(R.string.tangempay_cashback_widget_error_description),
                onClick = onReload,
                titleColor = ColorReference2 { TangemTheme.colors3.text.status.error },
                subtitleColor = ColorReference2 { TangemTheme.colors3.text.status.error },
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_arrow_refresh_20,
                    tintReference = { TangemTheme.colors3.icon.status.error },
                ),
            )
        }
    }
}