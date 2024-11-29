package com.tangem.features.onramp.main.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.onramp.impl.R

@Immutable
internal sealed interface OnrampMainComponentUM {

    val topBarConfig: OnrampMainTopBarUM
    val buyButtonConfig: BuyButtonConfig

    data class InitialLoading(
        val currency: String,
        val onClose: () -> Unit,
        val openSettings: () -> Unit,
    ) : OnrampMainComponentUM {
        override val topBarConfig: OnrampMainTopBarUM = OnrampMainTopBarUM(
            title = combinedReference(resourceReference(R.string.common_buy), stringReference(" $currency")),
            startButtonUM = TopAppBarButtonUM(
                iconRes = R.drawable.ic_close_24,
                onIconClicked = onClose,
                enabled = true,
            ),
            endButtonUM = TopAppBarButtonUM(
                iconRes = R.drawable.ic_more_vertical_24,
                onIconClicked = openSettings,
                enabled = false,
            ),
        )

        override val buyButtonConfig: BuyButtonConfig = BuyButtonConfig(
            text = resourceReference(R.string.common_buy),
            onClick = {},
            enabled = false,
        )
    }

    data class Content(
        override val topBarConfig: OnrampMainTopBarUM,
        override val buyButtonConfig: BuyButtonConfig,
        val amountBlockState: OnrampAmountBlockUM,
        val providerBlockState: OnrampProviderBlockUM,
    ) : OnrampMainComponentUM
}

internal data class BuyButtonConfig(
    val text: TextReference,
    val onClick: () -> Unit,
    val enabled: Boolean,
)