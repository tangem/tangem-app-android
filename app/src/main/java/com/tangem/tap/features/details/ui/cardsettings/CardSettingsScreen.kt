package com.tangem.tap.features.details.ui.cardsettings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.common.compose.TangemTypography
import com.tangem.tap.features.details.ui.common.DetailsMainButton
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R

@Composable
fun CardSettingsScreen(state: CardSettingsScreenState, onBackClick: () -> Unit) {
    val needReadCard = state.cardDetails == null

    SettingsScreensScaffold(
        content = {
            if (needReadCard) {
                CardSettingsReadCard(state.onScanCardClick)
            } else {
                CardSettings(state = state)
            }
        },
        titleRes = R.string.card_settings_title,
        backgroundColor = TangemTheme.colors.background.secondary,
        onBackClick = onBackClick,
    )
}

@Suppress("MagicNumber")
@Composable
fun CardSettingsReadCard(onScanCardClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp),
        ) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 80.dp, end = 80.dp, top = 70.dp)
                    .rotate(-15f),
                painter = painterResource(id = R.drawable.card_placeholder_secondary),
                contentDescription = "",
                contentScale = ContentScale.FillWidth,
            )
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 60.dp, end = 60.dp)
                    .rotate(-1f),
                painter = painterResource(id = R.drawable.card_placeholder_black),
                contentDescription = "",
                contentScale = ContentScale.FillWidth,
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
                color = colorResource(id = R.color.text_primary_1),
                style = TangemTypography.headline3,
            )
            Spacer(modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(id = R.string.scan_card_settings_message),
                color = colorResource(id = R.color.text_secondary),
                style = TangemTypography.body1,
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
fun CardSettings(state: CardSettingsScreenState) {
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
                is CardInfo.ResetToFactorySettings -> 28.dp
            }
            val paddingTop = when (it) {
                is CardInfo.CardId -> 0.dp
                is CardInfo.Issuer -> 12.dp
                is CardInfo.SignedHashes -> 12.dp
                is CardInfo.SecurityMode -> 14.dp
                is CardInfo.ChangeAccessCode -> 16.dp
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

@Composable
@Preview
private fun CardSettingsPreview() {
    CardSettingsScreen(state = CardSettingsScreenState(onScanCardClick = {}) {}, {})
}
