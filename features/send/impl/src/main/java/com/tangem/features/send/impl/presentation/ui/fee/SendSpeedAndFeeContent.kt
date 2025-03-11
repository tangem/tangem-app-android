package com.tangem.features.send.impl.presentation.ui.fee

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.state.previewdata.FeeStatePreviewData
import com.tangem.features.send.impl.presentation.state.previewdata.SendClickIntentsStub
import com.tangem.features.send.impl.presentation.ui.common.notifications
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents

private const val FEE_SELECTOR_KEY = "FEE_SELECTOR_KEY"
private const val FEE_CUSTOM_KEY = "FEE_CUSTOM_KEY"

@Composable
internal fun SendSpeedAndFeeContent(state: SendStates.FeeState?, clickIntents: SendClickIntents) {
    if (state == null) return
    val feeSendState = state.feeSelectorState as? FeeSelectorState.Content
    val notifications = state.notifications
    val isCustomSelected = feeSendState?.selectedFee == FeeType.Custom
    val hasNotifications = notifications.isNotEmpty()
    LazyColumn(
        modifier = Modifier // Do not put fillMaxSize() in here
            .background(TangemTheme.colors.background.tertiary)
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
    ) {
        feeSelector(state, clickIntents)
        if (feeSendState != null) {
            customFee(feeSendState = feeSendState, hasNotifications = hasNotifications)
        }
        notifications(notifications = notifications, hasPaddingAbove = isCustomSelected)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.feeSelector(state: SendStates.FeeState, clickIntents: SendClickIntents) {
    item(
        key = FEE_SELECTOR_KEY,
    ) {
        SendSpeedSelector(
            state = state,
            clickIntents = clickIntents,
            modifier = Modifier.animateItemPlacement(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.customFee(
    feeSendState: FeeSelectorState.Content,
    hasNotifications: Boolean,
    modifier: Modifier = Modifier,
) {
    item(
        key = FEE_CUSTOM_KEY,
    ) {
        SendCustomFee(
            customValues = feeSendState.customValues,
            selectedFee = feeSendState.selectedFee,
            hasNotifications = hasNotifications,
            modifier = modifier
                .fillMaxWidth()
                .animateItemPlacement()
                .background(TangemTheme.colors.background.tertiary)
                .padding(top = TangemTheme.dimens.spacing12),
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SendSpeedAndFeeContent_Preview(
    @PreviewParameter(FeeStatePreviewProvider::class) feeState: SendStates.FeeState,
) {
    TangemThemePreview {
        SendSpeedAndFeeContent(
            state = feeState,
            clickIntents = SendClickIntentsStub,
        )
    }
}

private class FeeStatePreviewProvider : PreviewParameterProvider<SendStates.FeeState> {
    override val values: Sequence<SendStates.FeeState>
        get() = sequenceOf(
            FeeStatePreviewData.feeState,
            FeeStatePreviewData.feeChoosableState,
            FeeStatePreviewData.feeCustomState,
            FeeStatePreviewData.errorFeeState,
        )
}
// endregion