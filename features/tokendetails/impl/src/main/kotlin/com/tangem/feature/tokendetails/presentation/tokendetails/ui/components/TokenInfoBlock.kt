package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TokenDetailsScreenTestTags
import com.tangem.core.ui.utils.getGreyScaleColorFilter
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenInfoBlockState
import com.tangem.features.tokendetails.impl.R

@Composable
internal fun TokenInfoBlock(state: TokenInfoBlockState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TangemTheme.dimens.size60),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1F),
        ) {
            Text(
                text = state.name,
                style = TangemTheme.typography.head,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.testTag(TokenDetailsScreenTestTags.TOKEN_TITLE),
            )
            NetworkInfoText(state.currency)
        }

        val (alpha, colorFilter) = remember(state.iconState.isGrayscale) {
            getGreyScaleColorFilter(state.iconState.isGrayscale)
        }
        CurrencyIcon(
            modifier = Modifier
                .size(TangemTheme.dimens.size48)
                .clip(TangemTheme.shapes.roundedCorners8),
            icon = state.iconState,
            alpha = alpha,
            colorFilter = colorFilter,
        )
    }
}

@Composable
private fun NetworkInfoText(currency: TokenInfoBlockState.Currency) {
    when (currency) {
        TokenInfoBlockState.Currency.Native -> {
            Text(
                text = stringResourceSafe(id = R.string.common_main_network),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption2,
            )
        }
        is TokenInfoBlockState.Currency.Token -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
            ) {
                val state = extractNetwork(tokenCurrency = currency)

                if (state.normalText.isNotBlank()) {
                    Text(
                        text = state.normalText,
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }

                Icon(
                    modifier = Modifier.size(TangemTheme.dimens.size16),
                    painter = painterResource(id = currency.networkIcon),
                    tint = Color.Unspecified,
                    contentDescription = null,
                )

                Text(
                    text = state.boldText,
                    style = TangemTheme.typography.caption1,
                    color = TangemTheme.colors.text.primary1,
                )
            }
        }
    }
}

private const val SEPARATOR = "%image%"

@Composable
private fun extractNetwork(tokenCurrency: TokenInfoBlockState.Currency.Token): ExtractedTokenNetworkText {
    val splitString = if (tokenCurrency.standardName != null) {
        stringResourceSafe(
            id = R.string.token_details_token_type_subtitle,
            formatArgs = arrayOf(
                tokenCurrency.standardName,
                tokenCurrency.networkName,
            ),
        )
    } else {
        stringResourceSafe(
            id = R.string.token_details_token_type_subtitle_no_standard,
            formatArgs = arrayOf(
                tokenCurrency.networkName,
            ),
        )
    }

    return remember(splitString) {
        ExtractedTokenNetworkText(
            normalText = splitString.substringBefore(delimiter = SEPARATOR, missingDelimiterValue = "").trim(),
            boldText = splitString.substringAfterLast(delimiter = SEPARATOR).trim(),
        )
    }
}

private data class ExtractedTokenNetworkText(
    val normalText: String,
    val boldText: String,
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "ja")
@Composable
private fun Preview_TokenInfoBlock(
    @PreviewParameter(TokenInfoStateProvider::class)
    state: TokenInfoBlockState,
) {
    TangemThemePreview {
        TokenInfoBlock(state, Modifier.background(TangemTheme.colors.background.secondary))
    }
}

private class TokenInfoStateProvider : CollectionPreviewParameterProvider<TokenInfoBlockState>(
    collection = listOf(
        TokenDetailsPreviewData.tokenInfoBlockState,
        TokenDetailsPreviewData.tokenInfoBlockStateWithLongName,
        TokenDetailsPreviewData.tokenInfoBlockStateWithLongNameInMainCurrency,
        TokenDetailsPreviewData.tokenInfoBlockStateWithLongNameNoStandard,
    ),
)