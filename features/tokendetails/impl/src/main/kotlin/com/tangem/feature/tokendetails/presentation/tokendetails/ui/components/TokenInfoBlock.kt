package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import coil.compose.rememberAsyncImagePainter
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenInfoBlockState
import com.tangem.features.tokendetails.impl.R

@Composable
internal fun TokenInfoBlock(state: TokenInfoBlockState, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.weight(1F),
        ) {
            Text(
                text = state.name,
                style = TangemTheme.typography.h1,
                color = TangemTheme.colors.text.primary1,
            )
            NetworkInfoText(state.currency)
        }

        val tokenIconPainter = when (LocalInspectionMode.current) {
            // show drawable res in preview
            true -> painterResource(id = R.drawable.img_stellar_22)
            false -> rememberAsyncImagePainter(model = state.iconUrl)
        }

        Image(
            modifier = Modifier.size(TangemTheme.dimens.size48),
            painter = tokenIconPainter,
            contentDescription = null,
        )
    }
}

@Composable
private fun NetworkInfoText(currency: TokenInfoBlockState.Currency) {
    when (currency) {
        TokenInfoBlockState.Currency.Native -> {
            Text(
                text = stringResource(id = R.string.common_main_network),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption,
            )
        }
        is TokenInfoBlockState.Currency.Token -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
            ) {
                val state = extractNetwork(tokenCurrency = currency)
                Text(
                    text = state.normalText,
                    style = TangemTheme.typography.caption,
                    color = TangemTheme.colors.text.tertiary,
                )
                Icon(
                    modifier = Modifier.size(TangemTheme.dimens.size16),
                    painter = painterResource(id = currency.networkIcon),
                    tint = Color.Unspecified,
                    contentDescription = null,
                )
                Text(
                    text = state.boldText,
                    style = TangemTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                    color = TangemTheme.colors.text.primary1,
                )
            }
        }
    }
}

private const val SEPARATOR = " %image% "

@Composable
private fun extractNetwork(tokenCurrency: TokenInfoBlockState.Currency.Token): ExtractedTokenNetworkText {
    val splitString = stringResource(
        id = R.string.token_details_token_type_subtitle,
        formatArgs = arrayOf(
            tokenCurrency.networkName,
            tokenCurrency.blockchainName,
        ),
    ).split(SEPARATOR)

    return remember(splitString) {
        ExtractedTokenNetworkText(
            normalText = splitString.firstOrNull().orEmpty(),
            boldText = splitString.getOrNull(1).orEmpty(),
        )
    }
}

private data class ExtractedTokenNetworkText(val normalText: String, val boldText: String)

@Preview
@Composable
private fun Preview_TokenInfoBlock_LightTheme(
    @PreviewParameter(TokenInfoStateProvider::class)
    state: TokenInfoBlockState,
) {
    TangemTheme(isDark = false) {
        TokenInfoBlock(state, Modifier.background(TangemTheme.colors.background.secondary))
    }
}

@Preview
@Composable
private fun Preview_TokenInfoBlock_DarkTheme(
    @PreviewParameter(TokenInfoStateProvider::class)
    state: TokenInfoBlockState,
) {
    TangemTheme(isDark = true) {
        TokenInfoBlock(state, Modifier.background(TangemTheme.colors.background.secondary))
    }
}

private class TokenInfoStateProvider : CollectionPreviewParameterProvider<TokenInfoBlockState>(
    collection = listOf(
        TokenDetailsPreviewData.tokenInfoBlockState,
        TokenDetailsPreviewData.tokenInfoBlockStateWithLongName,
        TokenDetailsPreviewData.tokenInfoBlockStateWithLongNameInMainCurrency,
    ),
)