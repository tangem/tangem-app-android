package com.tangem.features.send.v2.subcomponents.fee.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeType
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.features.send.v2.subcomponents.fee.model.SendFeeClickIntents
import com.tangem.features.send.v2.subcomponents.notifications

private const val FEE_SELECTOR_KEY = "FEE_SELECTOR_KEY"
private const val FEE_CUSTOM_KEY = "FEE_CUSTOM_KEY"

@Composable
internal fun SendFeeContent(state: FeeUM, clickIntents: SendFeeClickIntents) {
    if (state !is FeeUM.Content) return

    val notifications = state.notifications
    val feeSelectorUM = state.feeSelectorUM as? FeeSelectorUM.Content
    val isCustomSelected = feeSelectorUM?.selectedType == FeeType.Custom
    val hasNotifications = notifications.isNotEmpty()
    LazyColumn(
        modifier = Modifier // Do not put fillMaxSize() in here
            .background(TangemTheme.colors.background.tertiary)
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
            ),
    ) {
        feeSelector(state, clickIntents)
        if (feeSelectorUM != null) {
            customFee(
                feeSelectorUM = feeSelectorUM,
                onValueChange = clickIntents::onCustomFeeValueChange,
                onNonceChange = clickIntents::onNonceChange,
                nonce = state.nonce?.toString().orEmpty(),
                hasNotifications = hasNotifications,
            )
        }
        notifications(notifications = notifications, hasPaddingAbove = isCustomSelected)
    }
}

private fun LazyListScope.feeSelector(state: FeeUM.Content, clickIntents: SendFeeClickIntents) {
    item(key = FEE_SELECTOR_KEY) {
        SendSpeedSelector(
            state = state,
            clickIntents = clickIntents,
            modifier = Modifier.animateItem(),
        )
    }
}

internal fun LazyListScope.customFee(
    feeSelectorUM: FeeSelectorUM.Content,
    hasNotifications: Boolean,
    onValueChange: (Int, String) -> Unit,
    onNonceChange: (String) -> Unit,
    nonce: String?,
    modifier: Modifier = Modifier,
) {
    item(key = FEE_CUSTOM_KEY) {
        SendCustomFee(
            customValues = feeSelectorUM.customValues,
            selectedFee = feeSelectorUM.selectedType,
            hasNotifications = hasNotifications,
            onValueChange = onValueChange,
            onNonceChange = onNonceChange,
            nonce = nonce,
            modifier = modifier
                .fillMaxWidth()
                .animateItem()
                .background(TangemTheme.colors.background.tertiary)
                .padding(top = 12.dp),
        )
    }
}