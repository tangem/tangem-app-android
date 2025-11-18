package com.tangem.common.ui.amountScreen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.preview.AmountScreenClickIntentsStub
import com.tangem.common.ui.amountScreen.preview.AmountStatePreviewData
import com.tangem.common.ui.amountScreen.ui.amountFieldV2
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Amount screen with field
 * @param amountState amount state
 * @param clickIntents amount screen clicks
 */
@Composable
fun AmountScreenContent(
    amountState: AmountState,
    clickIntents: AmountScreenClickIntents,
    modifier: Modifier = Modifier,
    extraContent: (@Composable () -> Unit)? = null,
) {
    // Do not put fillMaxSize() in here
    LazyColumn(
        modifier = modifier
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        amountFieldV2(
            amountState = amountState,
            onValueChange = clickIntents::onAmountValueChange,
            onValuePastedTriggerDismiss = clickIntents::onAmountPasteTriggerDismiss,
            onCurrencyChange = clickIntents::onCurrencyChangeClick,
            onMaxAmountClick = clickIntents::onMaxValueClick,
        )
        if (extraContent != null) {
            item("EXTRA_CONTENT_KEY") {
                extraContent()
            }
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SendAmountContentPreview(
    @PreviewParameter(SendAmountContentPreviewProvider::class) amountState: AmountState,
) {
    TangemThemePreview {
        AmountScreenContent(
            amountState = amountState,
            clickIntents = AmountScreenClickIntentsStub,
        )
    }
}

private class SendAmountContentPreviewProvider : PreviewParameterProvider<AmountState> {
    override val values: Sequence<AmountState>
        get() = sequenceOf(
            AmountStatePreviewData.amountState,
            AmountStatePreviewData.amountStateV2,
        )
}
// endregion