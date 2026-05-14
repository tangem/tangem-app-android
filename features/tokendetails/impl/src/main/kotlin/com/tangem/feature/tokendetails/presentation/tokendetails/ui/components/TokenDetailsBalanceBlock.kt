package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.button.action.ActionButtons
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingAppBarBehavior
import com.tangem.core.ui.ds.topbar.collapsing.rememberTangemExitUntilCollapsedScrollBehavior
import com.tangem.core.ui.ds.topbar.collapsing.snapToExitUntilCollapsed
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalRootBackgroundColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceTypeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.features.tokendetails.impl.R
import kotlinx.collections.immutable.persistentListOf

private val CurrencyIconSize: Dp = 70.dp
private val NetworkBadgeSize: Dp = 24.dp
internal val TokenDetailsBalanceBlockHeight: Dp = 404.dp
private const val MIN_SCALE = 0.75f
private const val MAX_SCALE = 1f

@Composable
internal fun TokenDetailsBalanceBlock(
    balanceBlockUM: TokenDetailsBalanceBlockUM,
    behavior: TangemCollapsingAppBarBehavior,
    modifier: Modifier = Modifier,
) {
    val rootBackground by LocalRootBackgroundColor.current
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
            .padding(vertical = TangemTheme.dimens2.x10),
    ) {
        CurrencyIcon(
            state = balanceBlockUM.currencyIconState,
            shouldDisplayNetwork = true,
            iconSize = CurrencyIconSize,
            networkBadgeSize = NetworkBadgeSize,
            networkBadgeBackground = rootBackground,
        )
        SpacerH(TangemTheme.dimens2.x3)
        when (balanceBlockUM) {
            is TokenDetailsBalanceBlockUM.Content -> ContentBody(state = balanceBlockUM)
            is TokenDetailsBalanceBlockUM.Loading -> LoadingBody()
            is TokenDetailsBalanceBlockUM.Error -> ErrorBody()
        }
        SpacerH(TangemTheme.dimens2.x10)
        ActionButtons(buttons = balanceBlockUM.actionButtons)
    }
}

@Composable
private fun ContentBody(state: TokenDetailsBalanceBlockUM.Content) {
    AnimatedContent(
        targetState = state.tokenBalanceTypeUM.type,
        label = "Token balance type",
    ) { currentType ->
        val tokenBalanceTypeUM = state.tokenBalanceTypeUM
        when (tokenBalanceTypeUM) {
            is TokenBalanceTypeUM.Multiple -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
                modifier = Modifier.clickable(onClick = tokenBalanceTypeUM.onSelect),
            ) {
                Text(
                    text = currentType.text.resolveReference(),
                    style = TangemTheme.typography2.calloutSemibold15,
                    color = TangemTheme.colors2.text.neutral.secondary,
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_sort_24),
                    contentDescription = null,
                    tint = TangemTheme.colors2.graphic.neutral.secondary,
                    modifier = Modifier.size(TangemTheme.dimens2.x4),
                )
            }
            TokenBalanceTypeUM.Single -> Text(
                text = currentType.text.resolveReference(),
                style = TangemTheme.typography2.calloutSemibold15,
                color = TangemTheme.colors2.text.neutral.secondary,
            )
        }
    }
    SpacerH(TangemTheme.dimens2.x2)
    Text(
        text = state.displayFiatBalance.resolveAnnotatedReference(),
        style = TangemTheme.typography2.titleRegular44,
        color = TangemTheme.colors2.text.neutral.primary,
    )
    SpacerH(TangemTheme.dimens2.x2_5)
    Text(
        text = state.displayCryptoBalance.resolveAnnotatedReference(),
        style = TangemTheme.typography2.bodySemibold16,
        color = TangemTheme.colors2.text.neutral.secondary,
    )
}

@Composable
private fun LoadingBody() {
    Text(
        text = TokenBalanceTypeUM.Type.ALL.text.resolveReference(),
        style = TangemTheme.typography2.calloutSemibold15,
        color = TangemTheme.colors2.text.neutral.secondary,
    )
    SpacerH(TangemTheme.dimens2.x2)
    TextShimmer(
        style = TangemTheme.typography2.titleRegular44,
        text = "$1234567890",
        radius = TangemTheme.dimens2.x6,
    )
    SpacerH(TangemTheme.dimens2.x2)
    TextShimmer(
        style = TangemTheme.typography2.bodySemibold16,
        text = "12345.67",
        radius = TangemTheme.dimens2.x4,
    )
}

@Composable
private fun ErrorBody() {
    Text(
        text = "—",
        style = TangemTheme.typography2.titleRegular44,
        color = TangemTheme.colors2.text.neutral.primary,
    )
    SpacerH(TangemTheme.dimens2.x2_5)
    Text(
        text = "—",
        style = TangemTheme.typography2.bodySemibold16,
        color = TangemTheme.colors2.text.neutral.secondary,
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TokenDetailsBalanceBlock_Preview(
    @PreviewParameter(PreviewProvider::class) params: TokenDetailsBalanceBlockUM,
) {
    TangemThemePreviewRedesign {
        TokenDetailsBalanceBlock(
            balanceBlockUM = params,
            behavior = rememberTangemExitUntilCollapsedScrollBehavior(),
            modifier = Modifier.background(TangemTheme.colors2.surface.level2),
        )
    }
}

private class PreviewProvider : PreviewParameterProvider<TokenDetailsBalanceBlockUM> {

    private val previewActionButtons = persistentListOf(
        TangemButtonUM(
            text = stringReference("Add funds"),
            tangemIconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_arrow_down_24,
                tintReference = { TangemTheme.colors2.graphic.neutral.primary },
            ),
            onClick = { },
            isEnabled = true,
            type = TangemButtonType.Secondary,
        ),
        TangemButtonUM(
            text = stringReference("Transfer"),
            tangemIconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_arrow_up_24,
                tintReference = { TangemTheme.colors2.graphic.neutral.primary },
            ),
            onClick = { },
            isEnabled = true,
            type = TangemButtonType.Secondary,
        ),
    )

    override val values: Sequence<TokenDetailsBalanceBlockUM>
        get() = sequenceOf(
            TokenDetailsBalanceBlockUM.Content(
                actionButtons = previewActionButtons,
                tokenBalanceTypeUM = TokenBalanceTypeUM.Multiple(
                    type = TokenBalanceTypeUM.Type.ALL,
                    availableTypes = persistentListOf(
                        TokenBalanceTypeUM.Type.ALL,
                        TokenBalanceTypeUM.Type.AVAILABLE,
                    ),
                    onSelect = { },
                ),
                currencyIconState = CurrencyIconState.Loading,
                displayCryptoBalanceAll = stringReference("0.0613884 BTC"),
                displayFiatBalanceAll = stringReference("$12,380.94"),
                displayCryptoBalanceAvailable = stringReference("0.05 BTC"),
                displayFiatBalanceAvailable = stringReference("$10,000.00"),
                isBalanceFlickering = false,
            ),
            TokenDetailsBalanceBlockUM.Content(
                actionButtons = previewActionButtons,
                tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                currencyIconState = CurrencyIconState.Loading,
                displayCryptoBalanceAll = stringReference("123.456 USDT"),
                displayFiatBalanceAll = stringReference("$123.45"),
                displayCryptoBalanceAvailable = null,
                displayFiatBalanceAvailable = null,
                isBalanceFlickering = false,
            ),
            TokenDetailsBalanceBlockUM.Loading(
                actionButtons = previewActionButtons,
                tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                currencyIconState = CurrencyIconState.Loading,
            ),
            TokenDetailsBalanceBlockUM.Error(
                actionButtons = previewActionButtons,
                tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                currencyIconState = CurrencyIconState.Loading,
            ),
        )
}
// endregion