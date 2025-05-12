package com.tangem.common.ui.expressStatus

import android.content.res.Configuration
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.inputrow.InputRowBestRate
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun ExpressProvider(
    providerName: TextReference,
    providerType: TextReference,
    providerTxId: String?,
    imageUrl: String,
) {
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action),
    ) {
        Row(
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing12)
                .padding(horizontal = TangemTheme.dimens.spacing12)
                .fillMaxWidth(),
        ) {
            Text(
                text = stringResourceSafe(id = R.string.express_provider),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            SpacerWMax()
            if (!providerTxId.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            clipboardManager.setText(AnnotatedString(providerTxId))
                            Toast
                                .makeText(context, R.string.express_transaction_id_copied, Toast.LENGTH_SHORT)
                                .show()
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
                        text = stringResourceSafe(R.string.express_transaction_id, providerTxId),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExpressProvider_Preview() {
    TangemThemePreview {
        ExpressProvider(
            providerName = TextReference.Str("Changelly"),
            providerType = TextReference.Str("CEX"),
            providerTxId = "hjsbajcqbhjsbajcqbhjsbajcqbhjsbajcqbhjsbajcqb",
            imageUrl = "",
        )
    }
}