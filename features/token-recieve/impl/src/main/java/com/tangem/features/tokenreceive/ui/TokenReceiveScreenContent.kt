package com.tangem.features.tokenreceive.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.message.TangemMessage
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_copy_24
import com.tangem.features.tokenreceive.entity.ReceiveExtrasHint
import com.tangem.features.tokenreceive.impl.R
import com.tangem.features.tokenreceive.ui.state.ReceiveScreenUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TokenReceiveScreenContent(state: ReceiveScreenUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(state = state)
            SpacerH(24.dp)
            ReceiveAddressCard(
                address = state.address,
                iconState = state.iconState,
                extrasHint = state.extrasHint,
                ensName = state.ensName,
                networkNotice = state.networkNotice,
                onCopyAddressClick = state.onCopyAddressClick,
                onCopyEnsClick = state.onCopyEnsClick,
            )
            if (state.warnings.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 24.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    state.warnings.forEach { warning ->
                        key(warning) {
                            TangemMessage(config = warning.config, contentColor = Color.Transparent)
                        }
                    }
                }
            }
            SpacerH(32.dp)
        }
        TangemButton(
            text = resourceReference(R.string.common_copy_address),
            iconEnd = TangemIconUM.Icon(Icons.ic_copy_24),
            size = TangemButton.Size.X12,
            onClick = state.onCopyAddressClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun Header(state: ReceiveScreenUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.title.resolveReference(),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = state.subtitle.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 402)
@Preview(showBackground = true, widthDp = 402, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_TokenReceiveScreenContent() {
    TangemThemePreviewRedesign {
        TokenReceiveScreenContent(
            state = ReceiveScreenUM(
                title = stringReference("Tether • USDT"),
                subtitle = stringReference("Ethereum network • ERC20"),
                iconState = CurrencyIconState.Loading,
                address = "0x3987734c3379A3E2e3eF21c3C8090Cc7aBCD6260",
                ensName = "cryptomonster.eth",
                extrasHint = ReceiveExtrasHint.NoTag,
                networkNotice = stringReference("Send only USDT on Ethereum (ERC20) network"),
                warnings = persistentListOf(),
                onCopyAddressClick = {},
                onCopyEnsClick = {},
            ),
        )
    }
}