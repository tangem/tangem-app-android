package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.tangem.common.ui.notifications.CreatePaymentAccountNotification
import com.tangem.core.ui.components.notifications.NoteMigrationNotification
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.annotatedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.ForceDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.WalletNotificationTestTags
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
            when (item) {
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
                is WalletNotification.YieldBoostPromo -> {
                    Notification(
                        config = item.config.copy(title = annotatedReference(yieldBoostPromoTitle())),
                        modifier = modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                        iconTint = TangemTheme.colors.icon.accent,
                        subtitleColor = TangemTheme.colors.text.secondary,
                    )
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
                else -> DefaultWalletNotification(item = item, modifier = modifier)
            }
        },
    )
}

@Composable
private fun LazyItemScope.DefaultWalletNotification(item: WalletNotification, modifier: Modifier = Modifier) {
    val itemModifier = modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
    val taggedModifier = when (item) {
        is WalletNotification.AssetsDiscoveryCompleted ->
            itemModifier.testTag(WalletNotificationTestTags.ASSETS_DISCOVERY_BANNER)
        else -> itemModifier
    }
    Notification(
        config = item.config,
        modifier = taggedModifier,
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

@Composable
private fun yieldBoostPromoTitle(): AnnotatedString {
    val accent = TangemTheme.colors.text.accent
    val baseTitle = stringResourceSafe(com.tangem.core.res.R.string.yield_apy_boost_banner_title)
    val tailTitle = stringResourceSafe(com.tangem.core.res.R.string.yield_apy_boost_banner_title_apy_multiplied)
    return buildAnnotatedString {
        append(baseTitle)
        append(" · ")
        withStyle(SpanStyle(color = accent)) {
            append(tailTitle)
        }
    }
}