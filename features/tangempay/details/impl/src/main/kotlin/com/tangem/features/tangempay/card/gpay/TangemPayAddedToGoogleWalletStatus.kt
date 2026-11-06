package com.tangem.features.tangempay.card.gpay

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.details.impl.R

@Composable
internal fun TangemPayAddedToGoogleWalletStatus(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.img_google_wallet_20),
            contentDescription = null,
        )
        Text(
            text = stringResourceSafe(R.string.tangempay_card_details_added_to_google_wallet_text),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTangemPayAddedToGoogleWalletStatus() {
    TangemThemePreviewRedesign {
        TangemPayAddedToGoogleWalletStatus(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        )
    }
}