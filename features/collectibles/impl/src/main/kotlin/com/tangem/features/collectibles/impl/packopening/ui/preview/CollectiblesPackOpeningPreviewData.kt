package com.tangem.features.collectibles.impl.packopening.ui.preview

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM
import kotlinx.collections.immutable.persistentListOf

internal object CollectiblesPackOpeningPreviewData {

    val content = CollectiblesPackOpeningUM.Content(
        onBackClick = {},
        onCloseClick = {},
        order = CollectiblesPackOpeningUM.OrderUM(
            rows = persistentListOf(
                CollectiblesPackOpeningUM.PriceRowUM(labelRef = stringReference("Elite pack x1"), value = "$50"),
                CollectiblesPackOpeningUM.PriceRowUM(
                    labelRef = resourceReference(R.string.collectibles_confirm_order_platform_fee),
                    value = "$1",
                ),
                CollectiblesPackOpeningUM.PriceRowUM(
                    labelRef = resourceReference(R.string.collectibles_confirm_order_total),
                    value = "51 USDC",
                    isEmphasised = true,
                    hasDivider = false,
                ),
            ),
            button = CollectiblesPackOpeningUM.ButtonUM(
                textRef = resourceReference(R.string.collectibles_confirm_order_approve_button),
                isEnabled = true,
            ),
        ),
        onApproveClick = {},
        isPackOpened = false,
        isNavigationVisible = true,
    )

    val notEnoughFunds = content.copy(
        order = content.order.copy(
            button = CollectiblesPackOpeningUM.ButtonUM(
                textRef = resourceReference(R.string.collectibles_confirm_order_not_enough_funds_button),
                isEnabled = false,
            ),
        ),
    )

    val loading = CollectiblesPackOpeningUM.Loading(onBackClick = {}, onCloseClick = {})

    val error = CollectiblesPackOpeningUM.Error(
        onBackClick = {},
        onCloseClick = {},
        message = stringReference("Failed to load the pack.\nTap to reload"),
        onReloadClick = {},
    )
}