package com.tangem.features.tokenreceive.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TokenReceiveWarningBottomSheetTestTags
import com.tangem.features.tokenreceive.impl.R
import com.tangem.features.tokenreceive.ui.state.WarningUM

@Composable
internal fun TokenReceiveWarningContent(warningUM: WarningUM) {
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 28.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
            )
            .testTag(TokenReceiveWarningBottomSheetTestTags.BOTTOM_SHEET),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CurrencyIcon(
            modifier = Modifier
                .size(size = 76.dp),
            state = warningUM.iconState,
            shouldDisplayNetwork = true,
            networkBadgeSize = 24.dp,
            iconSize = 72.dp,
        )
        SpacerH(32.dp)
        WarningBlock(networkName = warningUM.network)
        SpacerH(8.dp)
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = TextAlign.Center,
            text = stringResourceSafe(R.string.domain_receive_assets_onboarding_description),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        SpacerH(48.dp)
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            variant = TangemButton.Variant.Secondary,
            text = resourceReference(R.string.common_got_it),
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                warningUM.onWarningAcknowledged()
            },
            size = TangemButton.Size.X12,
        )
    }
}

@Composable
private fun WarningBlock(networkName: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            textAlign = TextAlign.Center,
            text = stringResourceSafe(R.string.domain_receive_assets_onboarding_title),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
        )

        Text(
            textAlign = TextAlign.Center,
            text = stringResourceSafe(R.string.domain_receive_assets_onboarding_network_name, networkName),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_TokenReceiveWarningContent(
    @PreviewParameter(TokenReceiveWarningContentProvider::class) warningUM: WarningUM,
) {
    TangemThemePreviewRedesign {
        TokenReceiveWarningContent(warningUM = warningUM)
    }
}