package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import com.tangem.common.ui.notifications.CreatePaymentAccountNotification
import com.tangem.core.ui.components.notifications.NoteMigrationNotification
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.ForceDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import kotlinx.collections.immutable.ImmutableList

/**
 * Wallet notifications
 *
 * @param configs  list of notifications
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
internal fun LazyListScope.notifications(configs: ImmutableList<WalletNotification>, modifier: Modifier = Modifier) {
    items(
        items = configs,
        key = { it::class.java },
        contentType = { it::class.java },
        itemContent = { item ->
            // TODO develop promo banner general component
            when (item) {
                is WalletNotification.SwapPromo -> {
                    // Use it on new promo action
                }
                is WalletNotification.NoteMigration -> {
                    NoteMigrationNotification(
                        config = item.config,
                        modifier = modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                    )
                }
                is WalletNotification.FinishWalletActivation -> {
                    Notification(
                        config = item.config,
                        modifier = modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                    )
                }
                is WalletNotification.UpgradeHotWalletPromo -> {
                    ForceDarkTheme {
                        Notification(
                            config = item.config,
                            modifier = modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                        )
                    }
                }
                is WalletNotification.CreateTangemPayAccount -> {
                    CreatePaymentAccountNotification(
                        modifier = modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                        onClick = item.onClick,
                        onCloseClick = item.onCloseClick,
                        image = R.drawable.img_tangem_pay_visa_banner,
                        title = resourceReference(R.string.tangempay_onboarding_banner_title),
                        subtitle = resourceReference(R.string.tangempay_onboarding_banner_description),
                    )
                }
                else -> {
                    Notification(
                        config = item.config,
                        modifier = modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                        iconTint = when (item) {
                            is WalletNotification.Critical -> TangemTheme.colors.icon.warning
                            is WalletNotification.Informational -> TangemTheme.colors.icon.accent
                            is WalletNotification.RateApp -> TangemTheme.colors.icon.attention
                            is WalletNotification.UnlockWallets -> TangemTheme.colors.icon.primary1
                            is WalletNotification.UsedOutdatedData -> TangemTheme.colors.text.attention
                            else -> null
                        },
                        subtitleColor = TangemTheme.colors.text.secondary,
                    )
                }
            }
        },
    )
}