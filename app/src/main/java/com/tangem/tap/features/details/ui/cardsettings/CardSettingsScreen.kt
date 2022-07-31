package com.tangem.tap.features.details.ui.cardsettings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.tangem.tap.common.compose.TangemTypography
import com.tangem.tap.features.details.ui.common.DetailsMainButton
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R

@Composable
fun CardSettingsScreen(
    state: CardSettingsScreenState,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {

    SettingsScreensScaffold(
        content =
        if (state.cardDetails == null) {
            { CardSettingsReadCard(state.onScanCardClick, modifier = modifier) }
        } else {
            { CardSettings(state = state, modifier = modifier) }
        },
        titleRes = R.string.card_settings_title,
        onBackClick = onBackPressed,
    )
}

@Composable
fun CardSettingsReadCard(
    onScanCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {

        Box(
            modifier = modifier
                .fillMaxWidth(),

            ) {
            Image(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(start = 80.dp, end = 80.dp, top = 70.dp)
                    .rotate(-15f),
                painter = painterResource(id = R.drawable.card_placeholder_secondary),
                contentDescription = "",
                contentScale = ContentScale.FillWidth,
            )
            Image(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(start = 60.dp, end = 60.dp)
                    .rotate(-1f),
                painter = painterResource(id = R.drawable.card_placeholder_black),
                contentDescription = "",
                contentScale = ContentScale.FillWidth,
            )

        }


        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        ) {

            Text(
                text = stringResource(id = R.string.scan_card_settings_title),
                color = colorResource(id = R.color.text_primary_1),
                style = TangemTypography.headline3,
            )
            Spacer(modifier = modifier.size(20.dp))
            Text(
                text = stringResource(id = R.string.scan_card_settings_message),
                color = colorResource(id = R.color.text_secondary),
                style = TangemTypography.body1,
            )
            Spacer(modifier = modifier.size(29.dp))
            DetailsMainButton(
                title = stringResource(id = R.string.scan_card_settings_button),
                onClick = onScanCardClick,
                modifier = modifier,

                )
        }
    }
}

@Composable
fun CardSettings(
    state: CardSettingsScreenState,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {

        state.cardDetails?.map {
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
                modifier = modifier
                    .fillMaxWidth()

                    .clickable(
                        enabled = it.clickable,
                        onClick = { state.onElementClick(it) },
                    )
                    .padding(start = 20.dp, end = 20.dp, bottom = paddingBottom, top = paddingTop),
            ) {
                val titleColor = if (it.clickable)  R.color.text_primary_1 else R.color.text_tertiary
                val subtitleColor = if (it.clickable)  R.color.text_secondary else R.color.text_tertiary
                Text(
                    text = stringResource(id = it.titleRes),
                    color = colorResource(id = titleColor),
                    style = TangemTypography.subtitle1,
                )
                Spacer(modifier = modifier.size(4.dp))
                Text(
                    text = it.subtitle,
                    color = colorResource(id = subtitleColor),
                    style = TangemTypography.body2,
                )
            }

        }

    }
}

@Composable
@Preview
fun CardSettingsPreview() {
    CardSettingsScreen(state = CardSettingsScreenState(onScanCardClick = {}) {}, {})
}
