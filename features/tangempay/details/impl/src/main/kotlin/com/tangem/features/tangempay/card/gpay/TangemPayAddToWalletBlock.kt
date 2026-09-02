package com.tangem.features.tangempay.card.gpay

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.ds2.messagebanner.CloseButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.details.impl.R

@Composable
internal fun TangemPayAddToWalletBlock(state: AddToWalletBlockState, modifier: Modifier = Modifier) {
    TangemMessageBanner(
        title = resourceReference(R.string.tangempay_card_details_open_wallet_notification_title),
        modifier = modifier.testTag(TangemPayTestTags.ADD_TO_WALLET_BANNER),
        variant = TangemMessageBanner.Variant.Solid,
        onClick = state.onClick,
        description = resourceReference(R.string.tangempay_card_details_open_wallet_notification_subtitle),
        slotEnd = { TangemMessageBanner.CloseButton(onClick = state.onClickClose) },
    )
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTangemPayAddToWalletBlock() {
    TangemThemePreview {
        TangemPayAddToWalletBlock(AddToWalletBlockState(onClick = {}, onClickClose = {}))
    }
}