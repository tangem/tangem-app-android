package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButtonIconStart
import com.tangem.core.ui.components.buttons.common.TangemButtonIconType
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.AddToWalletBlockState

@Composable
internal fun TangemPayAddToWalletBlock(state: AddToWalletBlockState, modifier: Modifier = Modifier) {
    PrimaryButtonIconStart(
        modifier = modifier.fillMaxWidth(),
        iconResId = R.drawable.img_google_wallet,
        text = stringResourceSafe(R.string.tangempay_card_details_add_to_wallet_button_text),
        onClick = state.onClick,
        iconType = TangemButtonIconType.OriginalColor,
    )
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTangemPayAddToWalletBlock() {
    TangemThemePreview {
        TangemPayAddToWalletBlock(AddToWalletBlockState {})
    }
}