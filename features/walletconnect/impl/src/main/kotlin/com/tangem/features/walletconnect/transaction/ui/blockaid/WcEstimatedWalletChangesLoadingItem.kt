package com.tangem.features.walletconnect.transaction.ui.blockaid

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
internal fun WcEstimatedWalletChangesLoadingItem(modifier: Modifier = Modifier) {
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 14.dp),
        ) {
            CircularProgressIndicator(
                color = TangemTheme.colors.icon.accent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp),
            )
            Text(
                modifier = modifier
                    .fillMaxWidth(),
                text = stringResourceSafe(R.string.wc_common_loading),
                color = TangemTheme.colors.text.disabled,
                style = TangemTheme.typography.body2,
            )
        }
    }
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WcEstimatedWalletChangesLoadingItems() {
    TangemThemePreview {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary),
        ) {
            WcEstimatedWalletChangesLoadingItem()
        }
    }
}