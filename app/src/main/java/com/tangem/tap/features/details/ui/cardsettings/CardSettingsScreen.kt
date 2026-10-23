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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.DeviceSettingsScreenTestTags
import com.tangem.tap.features.details.ui.common.DetailsMainButton
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R

private const val CARD_PLACEHOLDER_SECONDARY_ROTATION = -15f
private const val CARD_PLACEHOLDER_PRIMARY_ROTATION = -1f

@Composable
internal fun CardSettingsScreen(state: CardSettingsScreenState, modifier: Modifier = Modifier) {
    val isCardReadingNeeded = state.cardDetails == null

    SettingsScreensScaffold(
        onBackClick = state.onBackClick,
        modifier = modifier,
        titleRes = R.string.card_settings_title,
        content = {
            if (isCardReadingNeeded) {
                CardSettingsReadCard(state.onScanCardClick)
            } else {
                CardSettings(state = state)
            }
        },
    )
}

@Composable
private fun CardSettingsReadCard(onScanCardClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        CardPlaceholderImages()
        Spacer(modifier = Modifier.weight(1f))
        ScanCardContent(onScanCardClick = onScanCardClick)
    }
}

@Composable
private fun CardPlaceholderImages() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 40.dp)
            .testTag(DeviceSettingsScreenTestTags.IMAGE_BLOCK),
    ) {
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 80.dp,
                    end = 80.dp,
                    top = 70.dp,
                )
                .rotate(CARD_PLACEHOLDER_SECONDARY_ROTATION),
            painter = painterResource(id = R.drawable.card_placeholder_secondary),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
        )
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 60.dp,
                    end = 60.dp,
                )
                .rotate(CARD_PLACEHOLDER_PRIMARY_ROTATION),
            painter = painterResource(id = R.drawable.card_placeholder_black),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
        )
    }
}

@Composable
private fun ScanCardContent(onScanCardClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 32.dp,
            ),
    ) {
        Text(
            text = stringResourceSafe(id = R.string.scan_card_settings_title),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.h3,
        )
        Spacer(modifier = Modifier.size(20.dp))
        Text(
            text = stringResourceSafe(id = R.string.scan_card_settings_message),
            color = TangemTheme.colors.text.secondary,
            style = TangemTheme.typography.body1,
        )
        Spacer(modifier = Modifier.size(32.dp))
        DetailsMainButton(
            title = stringResourceSafe(id = R.string.scan_card_settings_button),
            onClick = onScanCardClick,
        )
    }
}

@Suppress("ComplexMethod")
@Composable
private fun CardSettings(state: CardSettingsScreenState) {
    if (state.cardDetails == null) return

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DeviceSettingsScreenTestTags.LAZY_LIST),
    ) {
        items(state.cardDetails) { cardInfo ->
            val paddingBottom = when (cardInfo) {
                is CardInfo.CardId, is CardInfo.Issuer -> 12.dp
                is CardInfo.SignedHashes -> 14.dp
                is CardInfo.SecurityMode -> 16.dp
                is CardInfo.ChangeAccessCode -> 16.dp
                is CardInfo.AccessCodeRecovery -> 16.dp
                is CardInfo.ResetToFactorySettings -> 28.dp
            }
            val paddingTop = when (cardInfo) {
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
                        enabled = cardInfo.isClickable,
                        onClick = { state.onElementClick(cardInfo) },
                    )
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = paddingBottom,
                        top = paddingTop,
                    ),
            ) {
                val titleColor = if (cardInfo.isClickable) {
                    TangemTheme.colors.text.primary1
                } else {
                    TangemTheme.colors.text.tertiary
                }
                val subtitleColor = if (cardInfo.isClickable) {
                    TangemTheme.colors.text.secondary
                } else {
                    TangemTheme.colors.text.tertiary
                }
                Text(
                    text = cardInfo.titleRes.resolveReference(),
                    color = titleColor,
                    style = TangemTheme.typography.subtitle1,
                    modifier = Modifier.testTag(DeviceSettingsScreenTestTags.ITEM_TITLE),
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = cardInfo.subtitle.resolveReference(),
                    color = subtitleColor,
                    style = TangemTheme.typography.body2,
                    modifier = Modifier.testTag(DeviceSettingsScreenTestTags.ITEM_SUBTITLE),
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