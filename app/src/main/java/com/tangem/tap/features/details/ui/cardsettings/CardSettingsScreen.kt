package com.tangem.tap.features.details.ui.cardsettings

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.tap.features.details.ui.common.DetailsMainButton
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R

@Composable
internal fun CardSettingsScreen(state: CardSettingsScreenState, modifier: Modifier = Modifier) {
    val needReadCard = state.cardDetails == null

    SettingsScreensScaffold(
        modifier = modifier,
        content = {
            if (needReadCard) {
                CardSettingsReadCard(state.onScanCardClick)
            } else {
                CardSettings(state = state)
            }
        },
        titleRes = R.string.card_settings_title,
        onBackClick = state.onBackClick,
    )
}

@Suppress("MagicNumber")
@Composable
private fun CardSettingsReadCard(onScanCardClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = TangemTheme.dimens.spacing40),
        ) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = TangemTheme.dimens.spacing80,
                        end = TangemTheme.dimens.spacing80,
                        top = TangemTheme.dimens.spacing70,
                    )
                    .rotate(-15f),
                painter = painterResource(id = R.drawable.card_placeholder_secondary),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
            )
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = TangemTheme.dimens.spacing60,
                        end = TangemTheme.dimens.spacing60,
                    )
                    .rotate(-1f),
                painter = painterResource(id = R.drawable.card_placeholder_black),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    bottom = TangemTheme.dimens.spacing32,
                ),
        ) {
            Text(
                text = stringResourceSafe(id = R.string.scan_card_settings_title),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.h3,
            )
            Spacer(modifier = Modifier.size(TangemTheme.dimens.size20))
            Text(
                text = stringResourceSafe(id = R.string.scan_card_settings_message),
                color = TangemTheme.colors.text.secondary,
                style = TangemTheme.typography.body1,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(weight = 1f, fill = false),
            )
            Spacer(modifier = Modifier.size(TangemTheme.dimens.size32))
            DetailsMainButton(
                title = stringResourceSafe(id = R.string.scan_card_settings_button),
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
                is CardInfo.CardId, is CardInfo.Issuer -> TangemTheme.dimens.spacing12
                is CardInfo.SignedHashes -> TangemTheme.dimens.spacing14
                is CardInfo.SecurityMode -> TangemTheme.dimens.spacing16
                is CardInfo.ChangeAccessCode -> TangemTheme.dimens.spacing16
                is CardInfo.AccessCodeRecovery -> TangemTheme.dimens.spacing16
                is CardInfo.ResetToFactorySettings -> TangemTheme.dimens.spacing28
            }
            val paddingTop = when (it) {
                is CardInfo.CardId -> TangemTheme.dimens.spacing0
                is CardInfo.Issuer -> TangemTheme.dimens.spacing12
                is CardInfo.SignedHashes -> TangemTheme.dimens.spacing12
                is CardInfo.SecurityMode -> TangemTheme.dimens.spacing14
                is CardInfo.ChangeAccessCode -> TangemTheme.dimens.spacing16
                is CardInfo.AccessCodeRecovery -> TangemTheme.dimens.spacing16
                is CardInfo.ResetToFactorySettings -> TangemTheme.dimens.spacing16
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = it.clickable,
                        onClick = { state.onElementClick(it) },
                    )
                    .padding(
                        start = TangemTheme.dimens.spacing20,
                        end = TangemTheme.dimens.spacing20,
                        bottom = paddingBottom,
                        top = paddingTop,
                    ),
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
                Spacer(modifier = Modifier.size(TangemTheme.dimens.size4))
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
    CardSettingsScreen(state = CardSettingsScreenState(onBackClick = {}, onScanCardClick = {}, onElementClick = {}))
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CardSettingsScreenStatePreview() {
    TangemThemePreview {
        CardSettingsScreenStateSample()
    }
}
// endregion Preview