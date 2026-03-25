package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds.button.SecondaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemDeviceIcon
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingAppBarBehavior
import com.tangem.core.ui.ds.topbar.collapsing.rememberTangemExitUntilCollapsedScrollBehavior
import com.tangem.core.ui.ds.topbar.collapsing.snapToExitUntilCollapsed
import com.tangem.core.ui.extensions.orEmpty
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.feature.wallet.presentation.preview.WalletBalancePreview
import com.tangem.feature.wallet.presentation.preview.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletBalanceUM
import com.tangem.feature.wallet.presentation.wallet.ui.components.fastForEach
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.ImmutableList

private const val MIN_SCALE = 0.75f
private const val MAX_SCALE = 1f

@Composable
internal fun WalletBalance(
    walletBalanceUM: WalletBalanceUM,
    behavior: TangemCollapsingAppBarBehavior,
    buttons: ImmutableList<TangemButtonUM>,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    val collapsedFraction = behavior.state.collapsedFraction
    val alpha = 1f - collapsedFraction
    val scale = alpha.coerceIn(MIN_SCALE, MAX_SCALE)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .alpha(alpha)
            .scale(scale)
            .snapToExitUntilCollapsed(behavior)
            .fillMaxWidth()
            .padding(top = 64.dp)
            .statusBarsPadding(),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        ) {
            Balance(
                walletBalanceUM = walletBalanceUM,
                isBalanceHidden = isBalanceHidden,
            )
            SpacerH(TangemTheme.dimens2.x3)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
            ) {
                Text(
                    text = walletBalanceUM.name,
                    style = TangemTheme.typography2.bodyRegular14,
                    color = TangemTheme.colors2.text.neutral.tertiary,
                )
                TangemDeviceIcon(state = walletBalanceUM.deviceIcon)
            }
        }
        SpacerH(TangemTheme.dimens2.x2)
        ActionButtons(buttons)
        SpacerH(TangemTheme.dimens2.x6)
    }
}

@Composable
private fun Balance(walletBalanceUM: WalletBalanceUM, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = walletBalanceUM,
        label = "Update the balance",
        modifier = modifier.testTag(MainScreenTestTags.WALLET_BALANCE),
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 90)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 90))
        },
    ) { balanceUM ->
        when (balanceUM) {
            is WalletBalanceUM.Content -> {
                Text(
                    text = balanceUM.balance.orMaskWithStars(isBalanceHidden).resolveAnnotatedReference(),
                    style = TangemTheme.typography2.titleRegular44.applyBladeBrush(
                        isEnabled = balanceUM.isBalanceFlickering,
                        textColor = TangemTheme.colors2.text.neutral.primary,
                    ),
                    color = TangemTheme.colors2.text.neutral.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = TangemTheme.typography2.bodySemibold15.fontSize,
                        maxFontSize = TangemTheme.typography2.titleRegular44.fontSize,
                    ),
                )
            }
            is WalletBalanceUM.Error -> {
                Text(
                    text = StringsSigns.DASH_SIGN,
                    style = TangemTheme.typography2.titleRegular44,
                    color = TangemTheme.colors2.text.neutral.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = TangemTheme.typography2.bodySemibold15.fontSize,
                        maxFontSize = TangemTheme.typography2.titleRegular44.fontSize,
                    ),
                )
            }
            is WalletBalanceUM.Loading,
            -> {
                TextShimmer(
                    text = "123456",
                    style = TangemTheme.typography2.titleRegular44,
                    radius = TangemTheme.dimens2.x25,
                    textSizeHeight = true,
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(buttons: ImmutableList<TangemButtonUM>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        buttons.fastForEach { button ->
            key(button.text) {
                Column(
                    modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x2_5),
                    verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SecondaryTangemButton(
                        iconRes = button.iconRes,
                        onClick = button.onClick,
                        shape = TangemButtonShape.Rounded,
                    )
                    Text(
                        text = button.text.orEmpty().resolveReference(),
                        style = TangemTheme.typography2.bodySemibold15,
                        color = TangemTheme.colors2.text.neutral.primary,
                    )
                }
            }
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WalletBalance_Preview(@PreviewParameter(WalletBalancePreviewProvider::class) params: WalletBalanceUM) {
    TangemThemePreviewRedesign {
        WalletBalance(
            walletBalanceUM = params,
            behavior = rememberTangemExitUntilCollapsedScrollBehavior(),
            buttons = WalletPreviewData.actionButtons,
            isBalanceHidden = false,
        )
    }
}

private class WalletBalancePreviewProvider : PreviewParameterProvider<WalletBalanceUM> {
    override val values: Sequence<WalletBalanceUM>
        get() = sequenceOf(
            WalletBalancePreview.content,
            WalletBalancePreview.content.copy(isBalanceFlickering = true),
            WalletBalancePreview.loading,
            WalletBalancePreview.error,
        )
}
// endregion