package com.tangem.features.polymarket.impl.fasttrading.banner.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.messagebanner.CloseButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun PolymarketFastTradingBanner(state: PolymarketFastTradingBannerUM, modifier: Modifier = Modifier) {
    when (state) {
        is PolymarketFastTradingBannerUM.UnknownSigners -> TangemMessageBanner(
            title = resourceReference(R.string.prediction_session_key_unknown_signers_title),
            description = resourceReference(R.string.prediction_session_key_unknown_signers_text),
            variant = TangemMessageBanner.Variant.Warning,
            primaryButton = TangemMessageBanner.Button(
                text = resourceReference(R.string.prediction_session_key_review),
                onClick = state.onActionClick,
            ),
            modifier = modifier,
        )
        is PolymarketFastTradingBannerUM.Activate -> DismissibleBanner(
            title = resourceReference(R.string.prediction_session_key_banner_activate_title),
            description = resourceReference(R.string.prediction_session_key_banner_activate_text),
            buttonText = resourceReference(R.string.common_activate),
            onActionClick = state.onActionClick,
            onCloseClick = state.onCloseClick,
            modifier = modifier,
        )
        is PolymarketFastTradingBannerUM.Reactivate -> DismissibleBanner(
            title = resourceReference(R.string.prediction_session_key_banner_reactivate_title),
            description = resourceReference(R.string.prediction_session_key_banner_reactivate_text),
            buttonText = resourceReference(R.string.prediction_session_key_reactivate),
            onActionClick = state.onActionClick,
            onCloseClick = state.onCloseClick,
            modifier = modifier,
        )
        is PolymarketFastTradingBannerUM.Extend -> DismissibleBanner(
            title = pluralReference(
                id = R.plurals.prediction_session_key_banner_extend_title,
                count = state.daysLeft,
                formatArgs = wrappedList(state.daysLeft),
            ),
            description = resourceReference(R.string.prediction_session_key_banner_extend_text),
            buttonText = resourceReference(R.string.prediction_session_key_banner_extend_button),
            onActionClick = state.onActionClick,
            onCloseClick = state.onCloseClick,
            modifier = modifier,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun DismissibleBanner(
    title: TextReference,
    description: TextReference,
    buttonText: TextReference,
    onActionClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemMessageBanner(
        title = title,
        description = description,
        primaryButton = TangemMessageBanner.Button(text = buttonText, onClick = onActionClick),
        slotEnd = {
            TangemMessageBanner.CloseButton(
                onClick = onCloseClick,
                contentDescription = stringResourceSafe(R.string.common_close),
            )
        },
        modifier = modifier,
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketFastTradingBannerPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PolymarketFastTradingBanner(PolymarketFastTradingBannerUM.Activate(onActionClick = {}, onCloseClick = {}))
            PolymarketFastTradingBanner(PolymarketFastTradingBannerUM.Reactivate(onActionClick = {}, onCloseClick = {}))
            PolymarketFastTradingBanner(
                PolymarketFastTradingBannerUM.Extend(daysLeft = 12, onActionClick = {}, onCloseClick = {}),
            )
            PolymarketFastTradingBanner(PolymarketFastTradingBannerUM.UnknownSigners(onActionClick = {}))
        }
    }
}