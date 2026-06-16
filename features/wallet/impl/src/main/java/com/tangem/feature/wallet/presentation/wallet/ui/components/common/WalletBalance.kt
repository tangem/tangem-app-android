package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.button.action.ActionButtons
import com.tangem.core.ui.ds.image.TangemDeviceIcon
import com.tangem.core.ui.ds.placeholder.TextPlaceholder
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingAppBarBehavior
import com.tangem.core.ui.ds.topbar.collapsing.rememberTangemExitUntilCollapsedScrollBehavior
import com.tangem.core.ui.ds.topbar.collapsing.snapToExitUntilCollapsed
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.preview.WalletBalancePreview
import com.tangem.feature.wallet.presentation.preview.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAdditionalInfo
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletBalanceUM
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
    onSubtitleBottomChange: (Dp) -> Unit = {},
) {
    val collapsedFraction = behavior.state.collapsedFraction
    val alpha = 1f - collapsedFraction
    val scale = alpha.coerceIn(MIN_SCALE, MAX_SCALE)
    val density = LocalDensity.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .testTag(MainScreenTestTags.WALLET_LIST_ITEM)
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
                .padding(vertical = 58.dp),
        ) {
            Balance(
                walletBalanceUM = walletBalanceUM,
                isBalanceHidden = isBalanceHidden,
            )
            SpacerH(TangemTheme.dimens2.x3)
            Box(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    val rawBottomPx = coordinates.boundsInRoot().bottom
                    if (rawBottomPx <= 0f) return@onGloballyPositioned
                    onSubtitleBottomChange(with(density) { rawBottomPx.toDp() })
                },
            ) {
                SubtitleRow(walletBalanceUM = walletBalanceUM)
            }
        }
        SpacerH(TangemTheme.dimens2.x2)
        ActionButtons(buttons)
        SpacerH(TangemTheme.dimens2.x6)
    }
}

@Composable
private fun SubtitleRow(walletBalanceUM: WalletBalanceUM, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = walletBalanceUM.additionalInfo?.content,
        contentKey = { content ->
            when (content) {
                is WalletAdditionalInfo.Content.SyncProgress -> WalletAdditionalInfo.Content.SyncProgress::class
                else -> content
            }
        },
        label = "Update subtitle",
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 90)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 90))
        },
    ) { content ->
        when (content) {
            is WalletAdditionalInfo.Content.SyncProgress -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1_5),
                ) {
                    Text(
                        text = resourceReference(
                            id = R.string.initial_wallet_sync_restore_progress,
                            formatArgs = wrappedList(content.progressPercent),
                        ).resolveReference(),
                        style = TangemTheme.typography2.bodyRegular14,
                        color = TangemTheme.colors2.text.neutral.tertiary,
                        modifier = Modifier.testTag(MainScreenTestTags.SYNC_PROGRESS_TEXT),
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(19.dp),
                        color = TangemTheme.colors2.graphic.neutral.primary,
                        strokeWidth = 2.dp,
                    )
                }
            }
            else -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
                ) {
                    Text(
                        text = walletBalanceUM.name,
                        style = TangemTheme.typography2.bodyRegular14,
                        color = TangemTheme.colors2.text.neutral.tertiary,
                        modifier = Modifier.testTag(MainScreenTestTags.CARD_TITLE),
                    )
                    TangemDeviceIcon(
                        state = walletBalanceUM.deviceIcon,
                        modifier = Modifier.testTag(MainScreenTestTags.CARD_IMAGE),
                    )
                }
            }
        }
    }
}

@Composable
private fun Balance(walletBalanceUM: WalletBalanceUM, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = walletBalanceUM,
        contentKey = { it::class },
        label = "Update the balance",
        modifier = modifier.testTag(MainScreenTestTags.WALLET_BALANCE),
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 90)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 90))
        },
    ) { balanceUM ->
        when (balanceUM) {
            is WalletBalanceUM.Content -> Text(
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
            is WalletBalanceUM.Error -> Text(
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
            is WalletBalanceUM.Loading -> TextShimmer(
                text = "123456",
                style = TangemTheme.typography2.titleRegular44,
                radius = TangemTheme.dimens2.x25,
            )
            is WalletBalanceUM.Empty -> TextPlaceholder(
                textStyle = TangemTheme.typography2.titleRegular44,
                width = 200.dp,
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WalletBalance_Preview(
    @PreviewParameter(WalletBalancePreviewProvider::class)
    params: WalletBalancePreviewData,
) {
    TangemThemePreviewRedesign {
        WalletBalance(
            walletBalanceUM = params.walletBalanceUM,
            behavior = rememberTangemExitUntilCollapsedScrollBehavior(),
            buttons = params.actionsList,
            isBalanceHidden = false,
            modifier = Modifier.background(TangemTheme.colors2.surface.level1),
        )
    }
}

private data class WalletBalancePreviewData(
    val walletBalanceUM: WalletBalanceUM,
    val actionsList: ImmutableList<TangemButtonUM>,
)

private class WalletBalancePreviewProvider : PreviewParameterProvider<WalletBalancePreviewData> {
    override val values: Sequence<WalletBalancePreviewData>
        get() = sequenceOf(
            WalletBalancePreviewData(WalletBalancePreview.content, WalletPreviewData.actionButtons),
            WalletBalancePreviewData(WalletBalancePreview.syncProgress, WalletPreviewData.actionButtons),
            WalletBalancePreviewData(WalletBalancePreview.hiddenBalanceContent, WalletPreviewData.actionButtons),
            WalletBalancePreviewData(WalletBalancePreview.loading, WalletPreviewData.disabledActionButtons),
            WalletBalancePreviewData(WalletBalancePreview.error, WalletPreviewData.disabledActionButtons),
            WalletBalancePreviewData(WalletBalancePreview.empty, WalletPreviewData.disabledActionButtons),
        )
}
// endregion