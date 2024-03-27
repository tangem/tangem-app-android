package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.exchange

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.inputrow.InputRowBestRate
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun ExchangeProvider(
    providerName: TextReference,
    providerType: TextReference,
    providerTxId: String?,
    imageUrl: String,
) {
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action),
    ) {
        Box(
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing12)
                .padding(horizontal = TangemTheme.dimens.spacing12)
                .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(id = R.string.express_provider),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            if (!providerTxId.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            clipboardManager.setText(AnnotatedString(providerTxId))
                        },
                ) {
                    Icon(
                        modifier = Modifier
                            .size(TangemTheme.dimens.size20)
                            .padding(end = TangemTheme.dimens.spacing4)
                            .align(Alignment.CenterVertically),
                        painter = painterResource(id = R.drawable.ic_copy_24),
                        contentDescription = null,
                        tint = TangemTheme.colors.text.tertiary,
                    )
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        text = stringResource(R.string.express_transaction_id, providerTxId),
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }
            }
        }
        InputRowBestRate(
            imageUrl = imageUrl,
            title = providerName,
            titleExtra = providerType,
            subtitle = TextReference.Res(R.string.express_floating_rate),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExchangeProvider_Preview() {
    TangemTheme(isDark = false) {
        ExchangeProvider(
            providerName = TextReference.Str("Changelly"),
            providerType = TextReference.Str("CEX"),
            providerTxId = "hjsbajcqb",
            imageUrl = "",
        )
    }
}
