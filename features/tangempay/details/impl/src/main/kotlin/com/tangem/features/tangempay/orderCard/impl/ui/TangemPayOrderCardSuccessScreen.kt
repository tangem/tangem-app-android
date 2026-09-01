package com.tangem.features.tangempay.orderCard.impl.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardSuccessScreenUM
import com.tangem.features.tangempay.common.TangemPayResultScreen

@Composable
internal fun TangemPayOrderCardSuccessScreen(state: TangemPayOrderCardSuccessScreenUM, modifier: Modifier = Modifier) {
    val emailStyle = SpanStyle(color = TangemTheme.colors3.text.primary)
    TangemPayResultScreen(
        modifier = modifier,
        variant = TangemPayResultScreen.Variant.Success,
        title = resourceReference(R.string.tangempay_order_data_success_title),
        subtitle = pluralReference(
            id = R.plurals.tangempay_order_success_delivery_eta,
            count = state.deliveryEtaMaxBusinessDays,
            formatArgs = wrappedList(state.deliveryEtaMaxBusinessDays),
        ),
        footnote = resourceReference(
            id = R.string.tangempay_order_success_email_note,
            formatArgs = wrappedList(AnnotatedString(text = state.email, spanStyle = emailStyle)),
        ),
        buttonText = resourceReference(R.string.tangempay_order_success_show_card),
        onButtonClick = state.onShowCardClick,
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
        TangemPayOrderCardSuccessScreenUM(
            deliveryEtaMaxBusinessDays = PREVIEW_ETA_DAYS,
            email = PREVIEW_EMAIL,
            onShowCardClick = {},
        ),
        TangemPayOrderCardSuccessScreenUM(
            deliveryEtaMaxBusinessDays = 1,
            email = PREVIEW_EMAIL,
            onShowCardClick = {},
        ),
        TangemPayOrderCardSuccessScreenUM(
            deliveryEtaMaxBusinessDays = PREVIEW_ETA_DAYS,
            email = "johnny.silverhand.samurai@protonmail.com",
            onShowCardClick = {},
        ),
    ),
)