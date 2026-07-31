package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBarActionUM
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingAppBarBehavior
import com.tangem.core.ui.ds.topbar.collapsing.rememberTangemExitUntilCollapsedScrollBehavior
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemNavigationText
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTopBarConfig
import kotlinx.collections.immutable.persistentListOf

private const val VISIBILITY_THRESHOLD = 0.5f
private val BRAND_ICON_SLOT_SIZE = 44.dp

/**
 * Wallet screen collapsing top bar
 *
 * @param topBarConfig top bar config
 * @param walletBalance wallet balance text reference
 * @param isBalanceHidden whether the balance must be masked with stars
 * @param behavior collapsing behavior
 */
@Composable
internal fun WalletTopBar(
    topBarConfig: WalletTopBarConfig,
    walletBalance: TextReference?,
    isBalanceHidden: Boolean,
    behavior: TangemCollapsingAppBarBehavior,
) {
    val hapticManager = LocalHapticManager.current
    val isWrappedBalanceShown by remember {
        derivedStateOf { behavior.state.collapsedFraction > VISIBILITY_THRESHOLD }
    }

    val wrappedBalance = remember(walletBalance, isWrappedBalanceShown, isBalanceHidden) {
        walletBalance?.orMaskWithStars(isBalanceHidden).takeIf { isWrappedBalanceShown }
    }

    TangemTopNavigation(
        modifier = Modifier.testTag(MainScreenTestTags.TOP_BAR),
        contentAlign = TangemTopNavigation.ContentAlign.Center,
        startButton = {
            // Non-clickable brand mark, sized to align with the trailing action buttons.
            Box(modifier = Modifier.size(BRAND_ICON_SLOT_SIZE), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_tangem_24),
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.primary,
                )
            }
        },
        endButtonsGroup = if (topBarConfig.endActions.isNotEmpty()) {
            {
                topBarConfig.endActions.forEach { action ->
                    TangemButton(
                        variant = TangemButton.Variant.Ghost,
                        size = TangemButton.Size.X11,
                        iconStart = TangemIconUM.Icon(iconRes = action.iconRes),
                        isEnabled = action.isActionable && action.onClick != null,
                        onClick = {
                            hapticManager.perform(TangemHapticEffect.View.ContextClick)
                            action.onClick?.invoke()
                        },
                        modifier = Modifier.testTag(MainScreenTestTags.MORE_BUTTON),
                    )
                }
            }
        } else {
            null
        },
        isEndButtonsGroupBackgroundShown = isWrappedBalanceShown,
        fadeEnabled = isWrappedBalanceShown,
        contentColumn = {
            AnimatedVisibility(visible = wrappedBalance != null) {
                wrappedBalance?.let { balance ->
                    TangemNavigationText(text = balance, role = TangemNavigationText.Role.Title)
                }
            }
        },
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WalletTopBar_Preview() {
    TangemThemePreviewRedesign {
        WalletTopBar(
            topBarConfig = WalletTopBarConfig(),
            walletBalance = stringReference("$ 8923,05"),
            isBalanceHidden = false,
            behavior = rememberTangemExitUntilCollapsedScrollBehavior(),
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WalletTopBar_WithQrButton_Preview() {
    TangemThemePreviewRedesign {
        WalletTopBar(
            topBarConfig = WalletTopBarConfig(
                endActions = persistentListOf(
                    TangemTopBarActionUM(
                        iconRes = R.drawable.ic_qrcode_scaner_24,
                        onClick = {},
                    ),
                    TangemTopBarActionUM(
                        iconRes = R.drawable.ic_more_default_24,
                        onClick = {},
                    ),
                ),
            ),
            walletBalance = stringReference("$ 8923,05"),
            isBalanceHidden = false,
            behavior = rememberTangemExitUntilCollapsedScrollBehavior(),
        )
    }
}
// endregion