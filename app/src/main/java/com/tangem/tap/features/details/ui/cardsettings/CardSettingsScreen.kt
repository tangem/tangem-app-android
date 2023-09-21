package com.tangem.tap.features.details.ui.cardsettings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.userwallets.Artwork
import com.tangem.tap.features.details.ui.common.DetailsMainButton
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R

@Composable
internal fun CardSettingsScreen(
    state: CardSettingsScreenState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val needReadCard = state.cardDetails == null

    SettingsScreensScaffold(
        modifier = modifier,
        content = {
            if (needReadCard) {
                CardSettingsReadCard(state.onScanCardClick, state.cardImage)
            } else {
                CardSettings(state = state)
            }
        },
        titleRes = R.string.card_settings_title,
        onBackClick = onBackClick,
    )
}

@Suppress("MagicNumber")
@Composable
private fun CardSettingsReadCard(onScanCardClick: () -> Unit, cardArtwork: Artwork?) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = TangemTheme.dimens.size40),
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context = LocalContext.current)
                    .data(cardArtwork?.artworkId)
                    .crossfade(enable = true)
                    .build(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = TangemTheme.dimens.size16,
                        end = TangemTheme.dimens.size16,
                        top = TangemTheme.dimens.size70,
                    )
                    .fillMaxWidth(),
                loading = { },
                error = { },
                contentDescription = "",
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        ) {
            Text(
                text = stringResource(id = R.string.scan_card_settings_title),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.h3,
            )
            Spacer(modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(id = R.string.scan_card_settings_message),
                color = TangemTheme.colors.text.secondary,
                style = TangemTheme.typography.body1,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(weight = 1f, fill = false),
            )
            Spacer(modifier = Modifier.size(29.dp))
            DetailsMainButton(
                title = stringResource(id = R.string.scan_card_settings_button),
                onClick = onScanCardClick,
            )
        }
    }
}

@Suppress("ComplexMethod")
@Composable
private fun CardSettings(state: CardSettingsScreenState) {
    if (state.cardDetails == null) return

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(state.cardDetails) {
            val paddingBottom = when (it) {
                is CardInfo.CardId, is CardInfo.Issuer -> 12.dp
                is CardInfo.SignedHashes -> 14.dp
                is CardInfo.SecurityMode -> 16.dp
                is CardInfo.ChangeAccessCode -> 16.dp
                is CardInfo.AccessCodeRecovery -> 16.dp
                is CardInfo.ResetToFactorySettings -> 28.dp
            }
            val paddingTop = when (it) {
                is CardInfo.CardId -> 0.dp
                is CardInfo.Issuer -> 12.dp
                is CardInfo.SignedHashes -> 12.dp
                is CardInfo.SecurityMode -> 14.dp
                is CardInfo.ChangeAccessCode -> 16.dp
                is CardInfo.AccessCodeRecovery -> 16.dp
                is CardInfo.ResetToFactorySettings -> 16.dp
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = it.clickable,
                        onClick = { state.onElementClick(it) },
                    )
                    .padding(start = 20.dp, end = 20.dp, bottom = paddingBottom, top = paddingTop),
            ) {
                val titleColor = if (it.clickable) {
                    TangemTheme.colors.text.primary1
                } else {
                    TangemTheme.colors.text.tertiary
                }
                val subtitleColor = if (it.clickable) {
                    TangemTheme.colors.text.secondary
                } else {
                    TangemTheme.colors.text.tertiary
                }
                Text(
                    text = it.titleRes.resolveReference(),
                    color = titleColor,
                    style = TangemTheme.typography.subtitle1,
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = it.subtitle.resolveReference(),
                    color = subtitleColor,
                    style = TangemTheme.typography.body2,
                )
            }
        }
    }
}

// region Preview
@Composable
private fun CardSettingsScreenStateSample() {
    CardSettingsScreen(state = CardSettingsScreenState(onScanCardClick = {}, onElementClick = {}), {})
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CardSettingsScreenStatePreview_Light() {
    TangemTheme {
        CardSettingsScreenStateSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CardSettingsScreenStatePreview_Dark() {
    TangemTheme(isDark = true) {
        CardSettingsScreenStateSample()
    }
}
// endregion Preview
