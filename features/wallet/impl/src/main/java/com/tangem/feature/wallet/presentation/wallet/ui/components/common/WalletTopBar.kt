package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBarActionUM
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingAppBarBehavior
import com.tangem.core.ui.ds.topbar.collapsing.rememberTangemExitUntilCollapsedScrollBehavior
import com.tangem.core.ui.ds2.button.GroupEntry
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemNavigationText
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_24
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.core.ui.utils.zoomTransitionSource
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTopBarConfig
import kotlinx.collections.immutable.persistentListOf

private const val VISIBILITY_THRESHOLD = 0.5f

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
            WalletTopBarStartButton(
                onAllServicesClick = topBarConfig.onAllServicesClick,
                isMaterial = wrappedBalance != null,
            )
        },
        endButtonsGroup = if (topBarConfig.endActions.isNotEmpty()) {
            {
                topBarConfig.endActions.forEach { action ->
                    TangemButton.GroupEntry(
                        iconUM = TangemIconUM.Icon(iconRes = action.iconRes),
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

@Composable
private fun WalletTopBarStartButton(onAllServicesClick: (() -> Unit)?, isMaterial: Boolean) {
    if (onAllServicesClick == null) {
        // Non-clickable brand mark, sized to align with the trailing action buttons.
        Box(modifier = Modifier.size(TangemTopNavigation.ButtonSlotSize), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tangem_24),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.primary,
            )
        }
        return
    }

    val hapticManager = LocalHapticManager.current
    TangemButton(
        modifier = Modifier.zoomTransitionSource(
            key = WalletTopBarConfig.ALL_SERVICES_SHARED_TRANSITION_KEY,
            shape = CircleShape,
        ),
        variant = if (isMaterial) TangemButton.Variant.Material else TangemButton.Variant.Ghost,
        size = TangemButton.Size.X11,
        iconStart = TangemIconUM.Icon(imageVector = Icons.ic_sign_plus_24),
        contentDescription = stringResourceSafe(R.string.all_services_title),
        onClick = {
            hapticManager.perform(TangemHapticEffect.View.ContextClick)
            onAllServicesClick()
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
                onAllServicesClick = {},
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