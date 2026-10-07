package com.tangem.features.collectibles.impl.confirmorder.ui.preview

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.collectibles.impl.confirmorder.ui.state.CollectiblesConfirmOrderUM
import kotlinx.collections.immutable.persistentListOf

internal object CollectiblesConfirmOrderPreviewData {

    val content = CollectiblesConfirmOrderUM.Content(
        onBackClick = {},
        order = CollectiblesConfirmOrderUM.OrderUM(
            rows = persistentListOf(
                CollectiblesConfirmOrderUM.PriceRowUM(labelRef = stringReference("Elite pack x1"), value = "$50"),
                CollectiblesConfirmOrderUM.PriceRowUM(
                    labelRef = resourceReference(R.string.collectibles_confirm_order_platform_fee),
                    value = "$1",
                ),
                CollectiblesConfirmOrderUM.PriceRowUM(
                    labelRef = resourceReference(R.string.collectibles_confirm_order_total),
                    value = "51 USDC",
                    isEmphasised = true,
                    hasDivider = false,
                ),
            ),
            button = CollectiblesConfirmOrderUM.ButtonUM(
                textRef = resourceReference(R.string.collectibles_confirm_order_approve_button),
                isEnabled = true,
            ),
        ),
        onApproveClick = {},
    )

    val notEnoughFunds = content.copy(
        order = content.order.copy(
            button = CollectiblesConfirmOrderUM.ButtonUM(
                textRef = resourceReference(R.string.collectibles_confirm_order_not_enough_funds_button),
                isEnabled = false,
            ),
        ),
    )

    val loading = CollectiblesConfirmOrderUM.Loading(onBackClick = {})

    val error = CollectiblesConfirmOrderUM.Error(
        onBackClick = {},
        message = stringReference("Failed to load the pack.\nTap to reload"),
        onReloadClick = {},
    )
}