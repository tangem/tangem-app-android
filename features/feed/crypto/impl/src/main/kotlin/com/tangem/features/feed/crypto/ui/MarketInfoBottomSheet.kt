package com.tangem.features.feed.crypto.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.formatAnnotatedWithUrls
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.crypto.impl.R

@Composable
internal fun MarketInfoBottomSheet(onLinkClick: (url: String) -> Unit, onDismiss: () -> Unit) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        title = {
            TangemModal.TopNavigation(
                title = resourceReference(R.string.market_crypto_total_market_cap_title),
                onClose = onDismiss,
            )
        },
        content = {
            Column(modifier = Modifier.padding(all = 16.dp)) {
                Text(
                    text = formatAnnotatedWithUrls(
                        text = stringResourceSafe(R.string.market_crypto_total_cap_info),
                        linkColor = TangemTheme.colors3.text.accent.blue,
                        onLinkClick = onLinkClick,
                    ),
                    color = TangemTheme.colors3.text.secondary,
                    style = TangemTheme.typography3.subheading.medium,
                )
            }
        },
    )
}