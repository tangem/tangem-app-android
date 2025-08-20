package com.tangem.features.tokenreceive.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tokenreceive.impl.R
import com.tangem.features.tokenreceive.ui.state.WarningUM

@Composable
internal fun TokenReceiveWarningContent(warningUM: WarningUM) {
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = TangemTheme.colors.background.tertiary)
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CurrencyIcon(
            modifier = Modifier.size(size = 56.dp),
            state = warningUM.iconState,
            shouldDisplayNetwork = false,
            iconSize = 56.dp,
        )

        SpacerH24()

        WarningBlock(networkIcon = warningUM.networkIcon, networkName = warningUM.network)

        SpacerH12()

        Text(
            textAlign = TextAlign.Center,
            text = stringResourceSafe(R.string.domain_receive_assets_onboarding_description),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )

        SpacerH(48.dp)

        SecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResourceSafe(R.string.common_got_it),
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                warningUM.onWarningAcknowledged()
            },
        )
    }
}

@Composable
fun WarningBlock(networkName: String, networkIcon: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            textAlign = TextAlign.Center,
            text = stringResourceSafe(R.string.domain_receive_assets_onboarding_title),
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = networkIcon),
                tint = Color.Unspecified,
                contentDescription = null,
            )

            Text(
                textAlign = TextAlign.Center,
                text = stringResourceSafe(R.string.domain_receive_assets_onboarding_network_name, networkName),
                style = TangemTheme.typography.h3,
                color = TangemTheme.colors.text.primary1,
            )
        }
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_TokenReceiveWarningContent(
    @PreviewParameter(TokenReceiveWarningContentProvider::class) warningUM: WarningUM,
) {
    TangemThemePreview {
        TokenReceiveWarningContent(warningUM = warningUM)
    }
}

private class TokenReceiveWarningContentProvider : PreviewParameterProvider<WarningUM> {
    val iconState = CurrencyIconState.TokenIcon(
        url = null,
        topBadgeIconResId = null,
        fallbackTint = TangemColorPalette.Black,
        fallbackBackground = TangemColorPalette.Meadow,
        isGrayscale = false,
        showCustomBadge = false,
    )

    override val values: Sequence<WarningUM>
        get() = sequenceOf(
            WarningUM(
                iconState = iconState,
                onWarningAcknowledged = {},
                network = "Etherium",
                networkIcon = R.drawable.ic_eth_16,
            ),
        )
}