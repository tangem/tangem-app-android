package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarActionContent
import com.tangem.core.ui.ds.topbar.TangemTopBarActionUM
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingAppBarBehavior
import com.tangem.core.ui.ds.topbar.collapsing.rememberTangemExitUntilCollapsedScrollBehavior
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalRootBackgroundColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewDataLegacy
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTopBarConfig
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeTint
import kotlinx.collections.immutable.persistentListOf

private const val VISIBILITY_THRESHOLD = 0.5f

/**
 * Wallet screen collapsing top bar
 *
 * @param topBarConfig top bar config
 * @param walletBalance wallet balance text reference
 * @param behavior collapsing behavior
 */
@Composable
internal fun WalletTopBar(
    topBarConfig: WalletTopBarConfig,
    walletBalance: TextReference?,
    behavior: TangemCollapsingAppBarBehavior,
) {
    Surface(
        color = Color.Unspecified,
        contentColor = Color.Unspecified,
        modifier = Modifier.hazeEffectTangemTopBar(behavior),
    ) {
        val isWrappedBalanceShown by remember {
            derivedStateOf { behavior.state.collapsedFraction > VISIBILITY_THRESHOLD }
        }

        val wrappedBalance = remember(walletBalance, isWrappedBalanceShown) {
            walletBalance.takeIf { isWrappedBalanceShown }
        }

        TangemTopBar(
            title = wrappedBalance,
            startContent = {
                TangemTopBarActionContent(
                    TangemTopBarActionUM(
                        iconRes = R.drawable.ic_tangem_24,
                        isActionable = false,
                    ),
                )
            },
            endContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x5),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            lerp(
                                start = Color.Transparent,
                                stop = TangemTheme.colors2.button.backgroundSecondary,
                                fraction = behavior.state.collapsedFraction,
                            ),
                        ),
                ) {
                    topBarConfig.endActions.forEach { action ->
                        TangemTopBarActionContent(action)
                    }
                }
            },
            modifier = Modifier
                .statusBarsPadding()
                .testTag(MainScreenTestTags.TOP_BAR),
        )
    }
}

/**
 * Wallet screen top bar
 *
 * @param config component config
 */
@Suppress("MagicNumber")
@Deprecated("Remove with main toggle [DesignFeatureToggles.isRedesignEnabled]")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WalletTopBar(config: WalletTopBarConfig) {
    TopAppBar(
        title = {
            Icon(painter = painterResource(id = R.drawable.img_tangem_logo_90_24), contentDescription = null)
        },
        actions = {
            config.endActions.forEach { action ->
                action.onClick?.let { onClick ->
                    IconButton(onClick = onClick) {
                        Icon(
                            painter = painterResource(id = action.iconRes),
                            contentDescription = null,
                            modifier = Modifier
                                .rotate(if (action.iconRes == R.drawable.ic_more_default_24) 90f else 0f)
                                .testTag(MainScreenTestTags.MORE_BUTTON),
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TangemTheme.colors.background.secondary,
            titleContentColor = TangemTheme.colors.icon.primary1,
            actionIconContentColor = TangemTheme.colors.icon.primary1,
        ),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
        modifier = Modifier.testTag(MainScreenTestTags.TOP_BAR),
    )
}

@Composable
private fun Modifier.hazeEffectTangemTopBar(behavior: TangemCollapsingAppBarBehavior): Modifier {
    val rootBackground by LocalRootBackgroundColor.current
    val intensity by animateFloatAsState(targetValue = behavior.state.collapsedFraction * 2f)

    return hazeEffectTangem {
        fallbackTint = HazeTint(rootBackground.copy(alpha = intensity / 2))
        progressive = HazeProgressive.verticalGradient(
            startIntensity = intensity,
            endIntensity = 0f,
            preferPerformance = true,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_WalletTopBar() {
    TangemThemePreview {
        WalletTopBar(config = WalletPreviewDataLegacy.topBarConfig)
    }
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
            behavior = rememberTangemExitUntilCollapsedScrollBehavior(),
        )
    }
}
// endregion