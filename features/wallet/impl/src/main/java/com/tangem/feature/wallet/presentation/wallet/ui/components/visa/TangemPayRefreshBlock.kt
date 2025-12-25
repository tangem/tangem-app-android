package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.inputrow.InputRowImageBase
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification.Warning.TangemPayRefreshNeeded

@Composable
internal fun TangemPayRefreshBlock(state: TangemPayState.RefreshNeeded, modifier: Modifier = Modifier) {
    Column(modifier) {
        Notification(
            config = state.notification.config,
            iconTint = when (state.notification) {
                is WalletNotification.Critical -> TangemTheme.colors.icon.warning
                is WalletNotification.Informational -> TangemTheme.colors.icon.accent
                is WalletNotification.RateApp -> TangemTheme.colors.icon.attention
                is WalletNotification.UnlockWallets -> TangemTheme.colors.icon.primary1
                is WalletNotification.UsedOutdatedData -> TangemTheme.colors.text.attention
                else -> null
            },
        )
        SpacerH12()

        BlockCard(
            modifier = Modifier
                .clip(RoundedCornerShape(size = TangemTheme.dimens.radius14))
                .background(TangemTheme.colors.background.primary),
            enabled = false,
        ) {
            InputRowImageBase(
                modifier = Modifier.padding(
                    all = TangemTheme.dimens.spacing12,
                ),
                subtitle = resourceReference(R.string.tangempay_payment_account),
                caption = resourceReference(R.string.tangempay_payment_account_sync_needed),
                subtitleColor = TangemTheme.colors.text.tertiary,
                captionColor = TangemTheme.colors.text.tertiary,
                iconResWebp = R.drawable.img_visa_36,
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayRefreshBlockPreview() {
    TangemThemePreview {
        Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
            TangemPayRefreshBlock(
                state = TangemPayState.RefreshNeeded(
                    TangemPayRefreshNeeded(
                        tangemIcon = R.drawable.ic_tangem_24,
                        buttonText = resourceReference(id = R.string.tangempay_sync_needed_restore_access),
                        onRefreshClick = {},
                    ),
                ),
                modifier = Modifier,
            )
        }
    }
}