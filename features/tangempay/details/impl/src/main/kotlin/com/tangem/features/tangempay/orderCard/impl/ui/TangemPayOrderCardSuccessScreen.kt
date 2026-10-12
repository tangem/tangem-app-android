package com.tangem.features.tangempay.orderCard.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardSuccessScreenUM

@Composable
internal fun TangemPayOrderCardSuccessScreen(state: TangemPayOrderCardSuccessScreenUM, modifier: Modifier = Modifier) {
    val emailStyle = SpanStyle(color = TangemTheme.colors3.text.primary)
    TangemStatusScreen(
        modifier = modifier,
        variant = TangemStatusScreen.Variant.Success,
        title = resourceReference(R.string.tangempay_order_data_success_title),
        description = pluralReference(
            id = R.plurals.tangempay_order_success_delivery_eta,
            count = state.deliveryEtaMaxBusinessDays,
            formatArgs = wrappedList(state.deliveryEtaMaxBusinessDays),
        ),
        asset = {
            TangemStatusIcon(kind = TangemStatusIcon.Kind.Checkmark, modifier = Modifier.size(40.dp))
        },
        footer = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    text = resourceReference(
                        id = R.string.tangempay_order_success_email_note,
                        formatArgs = wrappedList(AnnotatedString(text = state.email, spanStyle = emailStyle)),
                    ).resolveAnnotatedReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                    textAlign = TextAlign.Center,
                )
                TangemButtonGroup(modifier = Modifier.fillMaxWidth()) {
                    TangemButton(
                        variant = TangemButton.Variant.Primary,
                        size = TangemButton.Size.X12,
                        text = state.buttonText,
                        onClick = state.onFinishClick,
                    )
                }
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Light")
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark",
)
@Composable
private fun TangemPayOrderCardSuccessScreenPreview(
    @PreviewParameter(OrderSuccessPreviewProvider::class) state: TangemPayOrderCardSuccessScreenUM,
) {
    TangemThemePreviewRedesign {
        TangemPayOrderCardSuccessScreen(state = state)
    }
}

private const val PREVIEW_ETA_DAYS = 20
private const val PREVIEW_EMAIL = "panampalmer@gmail.com"

private class OrderSuccessPreviewProvider : CollectionPreviewParameterProvider<TangemPayOrderCardSuccessScreenUM>(
    collection = listOf(
        previewOrderSuccess(),
        previewOrderSuccess(deliveryEtaMaxBusinessDays = 1),
        previewOrderSuccess(email = "johnny.silverhand.samurai@protonmail.com"),
        previewOrderSuccess(buttonText = resourceReference(R.string.common_close)),
    ),
)

private fun previewOrderSuccess(
    deliveryEtaMaxBusinessDays: Int = PREVIEW_ETA_DAYS,
    email: String = PREVIEW_EMAIL,
    buttonText: TextReference = resourceReference(R.string.tangempay_order_success_show_card),
) = TangemPayOrderCardSuccessScreenUM(
    deliveryEtaMaxBusinessDays = deliveryEtaMaxBusinessDays,
    email = email,
    buttonText = buttonText,
    onFinishClick = {},
)