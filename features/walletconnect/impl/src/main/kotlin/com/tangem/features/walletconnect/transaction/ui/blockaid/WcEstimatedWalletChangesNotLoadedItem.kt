package com.tangem.features.walletconnect.transaction.ui.blockaid

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.ui.common.WcSmallTitleItem

@Composable
internal fun WcEstimatedWalletChangesNotLoadedItem(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color = TangemTheme.colors.background.action)
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        WcSmallTitleItem(
            textRex = R.string.wc_estimated_wallet_changes,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Text(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 12.dp, bottom = 14.dp),
            text = stringResourceSafe(R.string.wc_estimated_wallet_changes_not_simulated),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.body2,
        )
    }
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WcEstimatedWalletChangesNotLoadedItems() {
    TangemThemePreview {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary),
        ) {
            WcEstimatedWalletChangesNotLoadedItem()
        }
    }
}