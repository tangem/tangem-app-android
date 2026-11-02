package com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.collectibles.impl.common.ui.CollectiblesBottomSheet
import com.tangem.features.collectibles.impl.common.ui.CollectiblesBottomSheetContent
import com.tangem.features.collectibles.impl.common.ui.CollectiblesBottomSheetPrimaryButton

/**
 * @param amount the payout, signed and formatted with its token
 * @param price what one more spin costs, formatted with its token
 */
@Composable
internal fun CollectiblesAutoBuybackSheet(
    amount: String,
    price: String,
    onOpenAgainClick: () -> Unit,
    onExplorePacksClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    CollectiblesBottomSheet(onDismiss = onDismiss) {
        AutoBuybackContent(
            amount = amount,
            price = price,
            onOpenAgainClick = onOpenAgainClick,
            onExplorePacksClick = onExplorePacksClick,
        )
    }
}

@Composable
private fun AutoBuybackContent(
    amount: String,
    price: String,
    onOpenAgainClick: () -> Unit,
    onExplorePacksClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CollectiblesBottomSheetContent(
        variant = TangemModalStatus.Variant.Success,
        icon = null,
        title = resourceReference(R.string.collectibles_auto_buyback_title),
        description = resourceReference(R.string.collectibles_auto_buyback_message, wrappedList(amount)),
        modifier = modifier,
    ) {
        TangemButton(
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.collectibles_low_inventory_explore_packs),
            onClick = onExplorePacksClick,
        )
        CollectiblesBottomSheetPrimaryButton(
            text = resourceReference(R.string.collectibles_auto_buyback_open_again_button, wrappedList(price)),
            onClick = onOpenAgainClick,
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesAutoBuybackSheet_Preview() {
    TangemThemePreviewRedesign {
        AutoBuybackContent(
            amount = "+35.60 USDC",
            price = "50 USDC",
            onOpenAgainClick = {},
            onExplorePacksClick = {},
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        )
    }
}