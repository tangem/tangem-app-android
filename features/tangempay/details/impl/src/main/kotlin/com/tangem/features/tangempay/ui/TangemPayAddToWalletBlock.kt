package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.ds.message.TangemMessage
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.AddToWalletBlockState

@Composable
internal fun TangemPayAddToWalletBlock(state: AddToWalletBlockState, modifier: Modifier = Modifier) {
    TangemMessage(
        modifier = modifier.clickableSingle(onClick = state.onClick),
        onCloseClick = state.onClickClose,
        title = resourceReference(R.string.tangempay_card_details_open_wallet_notification_title),
        subtitle = resourceReference(R.string.tangempay_card_details_open_wallet_notification_subtitle),
        messageEffect = TangemMessageEffect.Magic,
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