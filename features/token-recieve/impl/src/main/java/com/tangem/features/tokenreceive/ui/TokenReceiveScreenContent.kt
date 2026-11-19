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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_copy_24
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.features.tokenreceive.entity.ReceiveExtrasHint
import com.tangem.features.tokenreceive.impl.R
import com.tangem.features.tokenreceive.ui.state.ReceiveAddressUM
import com.tangem.features.tokenreceive.ui.state.ReceiveScreenUM
import kotlinx.collections.immutable.ImmutableList
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
            val reveal = rememberNoticeReveal()
            val singleAddress = state.addresses.singleOrNull()
            if (singleAddress != null) {
                SpacerH(24.dp)
                ReceiveAddressCard(
                    address = singleAddress,
                    iconState = state.iconState,
                    extrasHint = state.extrasHint,
                    networkNotice = state.networkNotice,
                    reveal = reveal,
                    onCopyEnsClick = state.onCopyEnsClick,
                )
            } else if (state.addresses.isNotEmpty()) {
                SpacerH(16.dp)
                ReceiveAddressPager(state = state, reveal = reveal)
            }
            SpacerH(32.dp)
        }
        Warnings(warnings = state.warnings)
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
private fun Warnings(warnings: ImmutableList<NotificationUM>) {
    if (warnings.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        warnings.forEach { warning ->
            key(warning) {
                val config = warning.config
                TangemMessageBanner(
                    title = config.title ?: config.subtitle,
                    description = config.subtitle.takeIf { config.title != null },
                    slotEnd = {
                        TangemIcon(
                            TangemIconUM.Icon(
                                imageVector = Icons.ic_info_20,
                                tintReference = { TangemTheme.colors3.icon.primary },
                            ),
                        )
                    },
                )
            }
        }
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
                title = stringReference("Bitcoin • BTC"),
                subtitle = stringReference("Bitcoin network"),
                iconState = CurrencyIconState.Loading,
                addresses = persistentListOf(
                    ReceiveAddressUM(
                        value = "bc1q44yecmh2ct4u6rgqrzhqvwc636ej2r240yqts6",
                        label = stringReference("Bitcoin address"),
                        isDynamic = false,
                        ensName = null,
                        onCopyClick = {},
                    ),
                    ReceiveAddressUM(
                        value = "1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2",
                        label = stringReference("Legacy Bitcoin Address"),
                        isDynamic = false,
                        ensName = null,
                        onCopyClick = {},
                    ),
                ),
                extrasHint = ReceiveExtrasHint.None,
                networkNotice = stringReference("Send only BTC on Bitcoin network"),
                warnings = persistentListOf(),
                onCopyAddressClick = {},
                onCopyEnsClick = {},
                onAddressPageChange = {},
            ),
        )
    }
}