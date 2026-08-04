package com.tangem.features.tangempay.multichain.othernetworks

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_28
import com.tangem.features.tangempay.details.impl.R

@Composable
internal fun PaymentOtherNetworksContent(state: PaymentOtherNetworksUM) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onClose,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = state.onClose,
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopBar(
                title = resourceReference(R.string.tangempay_other_networks_header),
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_close_24),
                        onClick = state.onClose,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                },
            )
        },
        content = { PaymentOtherNetworksBody(state = state) },
    )
}

@Composable
private fun PaymentOtherNetworksBody(state: PaymentOtherNetworksUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(color = TangemTheme.colors3.bg.status.infoSubtle, shape = CircleShape)
                .size(80.dp),
        ) {
            Icon(
                imageVector = Icons.ic_info_28,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.status.info,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = state.title.resolveReference(),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
        )
        Text(
            text = state.subtitle.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        TangemButton(
            text = resourceReference(R.string.common_close),
            onClick = state.onClose,
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
        )
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PaymentOtherNetworksContentPreview() {
    TangemThemePreviewRedesign {
        PaymentOtherNetworksContent(
            state = PaymentOtherNetworksUM(
                title = resourceReference(R.string.tangempay_other_networks_title),
                subtitle = resourceReference(R.string.tangempay_other_networks_subtitle),
                onClose = {},
            ),
        )
    }
}