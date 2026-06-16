package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.testTag
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
import com.tangem.core.ui.components.text.TextAnimatedCounter
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.button.action.ActionButtons
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TokenDetailsScreenTestTags
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceTypeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.features.tokendetails.impl.R
import kotlinx.collections.immutable.persistentListOf

private val CurrencyIconSize: Dp = 70.dp
private val NetworkBadgeSize: Dp = 24.dp

/** OpenType "tabular figures" feature — makes every digit the same width to prevent horizontal jitter. */
private const val TABULAR_FIGURES_FEATURE = "tnum"

@Composable
internal fun TokenDetailsBalanceBlock(
    balanceBlockUM: TokenDetailsBalanceBlockUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TangemTheme.dimens2.x10),
    ) {
        CurrencyIcon(
            state = balanceBlockUM.currencyIconState,
            shouldDisplayNetwork = true,
            iconSize = CurrencyIconSize,
            networkBadgeSize = NetworkBadgeSize,
            networkBadgeBackground = TangemTheme.colors2.surface.level2,
        )
        SpacerH(TangemTheme.dimens2.x3)
        when (balanceBlockUM) {
            is TokenDetailsBalanceBlockUM.Content -> ContentBody(
                state = balanceBlockUM,
                isBalanceHidden = isBalanceHidden,
            )
            is TokenDetailsBalanceBlockUM.Loading -> LoadingBody()
            is TokenDetailsBalanceBlockUM.Error -> ErrorBody()
        }
        if (!balanceBlockUM.isBalanceZeroContent()) {
            SpacerH(TangemTheme.dimens2.x10)
            val buttons = remember(
                balanceBlockUM.addFundsButton,
                balanceBlockUM.swapButton,
                balanceBlockUM.transferButton,
            ) {
                persistentListOf(
                    balanceBlockUM.addFundsButton,
                    balanceBlockUM.swapButton,
                    balanceBlockUM.transferButton,
                )
            }
            ActionButtons(buttons = buttons)
        }
    }
}

private fun TokenDetailsBalanceBlockUM.isBalanceZeroContent(): Boolean {
    return (this as? TokenDetailsBalanceBlockUM.Content)?.isBalanceZero == true
}

@Composable
private fun ContentBody(state: TokenDetailsBalanceBlockUM.Content, isBalanceHidden: Boolean) {
    AnimatedContent(
        targetState = state.tokenBalanceTypeUM.type,
        label = "Token balance type",
    ) { currentType ->
        when (val tokenBalanceTypeUM = state.tokenBalanceTypeUM) {
            is TokenBalanceTypeUM.Multiple -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
                modifier = Modifier.clickable(onClick = tokenBalanceTypeUM.onSelect),
            ) {
                Text(
                    text = currentType.text.resolveReference(),
                    style = TangemTheme.typography2.subheadlineMedium14,
                    color = TangemTheme.colors2.text.neutral.primary,
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_sort_24),
                    contentDescription = null,
                    tint = TangemTheme.colors2.graphic.neutral.secondary,
                    modifier = Modifier.size(TangemTheme.dimens2.x5),
                )
            }
            TokenBalanceTypeUM.Single -> Text(
                text = currentType.text.resolveReference(),
                style = TangemTheme.typography2.subheadlineMedium14,
                color = TangemTheme.colors2.text.neutral.primary,
            )
        }
    }
    SpacerH(TangemTheme.dimens2.x2)
    AnimatedBalance(
        modifier = Modifier.testTag(TokenDetailsScreenTestTags.BALANCE_FIAT),
        yieldBalance = state.displayYieldSupplyFiatBalance,
        fallbackBalance = state.displayFiatBalance,
        style = TangemTheme.typography2.titleRegular44,
        color = TangemTheme.colors2.text.neutral.primary,
        isBalanceHidden = isBalanceHidden,
    )
    SpacerH(TangemTheme.dimens2.x2_5)
    AnimatedBalance(
        yieldBalance = state.displayYieldSupplyCryptoBalance,
        fallbackBalance = state.displayCryptoBalance,
        style = TangemTheme.typography2.bodySemibold16,
        color = TangemTheme.colors2.text.neutral.secondary,
        isBalanceHidden = isBalanceHidden,
    )
}

/**
 * Renders a balance that animates digit-by-digit ([TextAnimatedCounter]) while a ticking yield supply
 * value is present, and falls back to a plain [Text] otherwise (or when the balance is hidden).
 */
@Composable
private fun AnimatedBalance(
    yieldBalance: String?,
    fallbackBalance: TextReference,
    style: TextStyle,
    color: Color,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens2.x6),
        contentAlignment = Alignment.Center,
    ) {
        if (yieldBalance != null && !isBalanceHidden) {
            TextAnimatedCounter(
                text = yieldBalance,
                // Tabular figures keep every digit the same width, so the centered balance doesn't
                // jitter horizontally as digits roll during the increment animation.
                style = style.copy(color = color, fontFeatureSettings = TABULAR_FIGURES_FEATURE),
            )
        } else {
            Text(
                text = fallbackBalance.orMaskWithStars(isBalanceHidden).resolveAnnotatedReference(),
                style = style,
                color = color,
            )
        }
    }
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
            isBalanceHidden = false,
            modifier = Modifier.background(TangemTheme.colors2.surface.level2),
        )
    }
}

private class PreviewProvider : PreviewParameterProvider<TokenDetailsBalanceBlockUM> {

    private val previewAddFundsButton = TangemButtonUM(
        text = stringReference("Add funds"),
        tangemIconUM = TangemIconUM.Icon(
            iconRes = R.drawable.ic_arrow_down_24,
            tintReference = { TangemTheme.colors2.graphic.neutral.primary },
        ),
        onClick = { },
        isEnabled = true,
        type = TangemButtonType.Secondary,
    )

    private val previewSwapButton = TangemButtonUM(
        text = stringReference("Swap"),
        tangemIconUM = TangemIconUM.Icon(
            iconRes = R.drawable.ic_exchange_default_24,
            tintReference = { TangemTheme.colors2.graphic.neutral.primary },
        ),
        onClick = { },
        isEnabled = true,
        type = TangemButtonType.Secondary,
    )

    private val previewTransferButton = TangemButtonUM(
        text = stringReference("Transfer"),
        tangemIconUM = TangemIconUM.Icon(
            iconRes = R.drawable.ic_arrow_up_24,
            tintReference = { TangemTheme.colors2.graphic.neutral.primary },
        ),
        onClick = { },
        isEnabled = true,
        type = TangemButtonType.Secondary,
    )

    override val values: Sequence<TokenDetailsBalanceBlockUM>
        get() = sequenceOf(
            TokenDetailsBalanceBlockUM.Content(
                addFundsButton = previewAddFundsButton,
                swapButton = previewSwapButton,
                transferButton = previewTransferButton,
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
                isBalanceZero = false,
            ),
            TokenDetailsBalanceBlockUM.Content(
                addFundsButton = previewAddFundsButton,
                swapButton = previewSwapButton,
                transferButton = previewTransferButton,
                tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                currencyIconState = CurrencyIconState.Loading,
                displayCryptoBalanceAll = stringReference("123.456 USDT"),
                displayFiatBalanceAll = stringReference("$123.45"),
                displayCryptoBalanceAvailable = null,
                displayFiatBalanceAvailable = null,
                isBalanceFlickering = false,
                isBalanceZero = false,
            ),
            TokenDetailsBalanceBlockUM.Content(
                addFundsButton = previewAddFundsButton,
                swapButton = previewSwapButton,
                transferButton = previewTransferButton,
                tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                currencyIconState = CurrencyIconState.Loading,
                displayCryptoBalanceAll = stringReference("0 USDT"),
                displayFiatBalanceAll = stringReference("$0.00"),
                displayCryptoBalanceAvailable = null,
                displayFiatBalanceAvailable = null,
                isBalanceFlickering = false,
                isBalanceZero = true,
            ),
            TokenDetailsBalanceBlockUM.Loading(
                addFundsButton = previewAddFundsButton,
                swapButton = previewSwapButton,
                transferButton = previewTransferButton,
                tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                currencyIconState = CurrencyIconState.Loading,
            ),
            TokenDetailsBalanceBlockUM.Error(
                addFundsButton = previewAddFundsButton,
                swapButton = previewSwapButton,
                transferButton = previewTransferButton,
                tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                currencyIconState = CurrencyIconState.Loading,
            ),
        )
}
// endregion