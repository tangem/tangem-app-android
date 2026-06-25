package com.tangem.features.send.subcomponents.amount

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.api.subcomponents.amount.SendAmountComponent
import com.tangem.features.send.api.subcomponents.amount.SendAmountComponentParams
import com.tangem.features.send.impl.R
import com.tangem.features.send.subcomponents.amount.model.SendAmountModel
import com.tangem.features.send.subcomponents.amount.ui.SendAmountContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSendAmountComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SendAmountComponentParams.AmountParams,
) : SendAmountComponent, AppComponentContext by appComponentContext {

    private val model: SendAmountModel = getOrCreateModel(params = params)

    override fun updateState(amountUM: AmountState) = model.updateState(amountUM)

    @Composable
    override fun Title() {
        AppBarWithBackButtonAndIcon(
            text = stringResourceSafe(R.string.send_amount_label),
            onBackClick = {
                params.callback.onBackClick(params.route)
            },
            backIconRes = if (params.route.isEditMode) {
                R.drawable.ic_back_24
            } else {
                R.drawable.ic_close_24
            },
            backgroundColor = TangemTheme.colors.background.tertiary,
            modifier = Modifier.height(TangemTheme.dimens.size56),
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val isSendWithSwapAvailable by model.isSendWithSwapAvailable.collectAsStateWithLifecycle()

        SendAmountContent(
            amountState = state,
            clickIntents = model,
            isSendWithSwapAvailable = isSendWithSwapAvailable,
            modifier = modifier,
        )
    }

    @Composable
    override fun Footer() {
        val state by model.uiState.collectAsStateWithLifecycle()
        PrimaryButton(
            text = if (params.route.isEditMode) {
                stringResourceSafe(R.string.common_continue)
            } else {
                stringResourceSafe(R.string.common_next)
            },
            enabled = state.isPrimaryButtonEnabled,
            onClick = {
                model.onAmountNext()
                params.callback.onNextClick(params.route)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
        )
    }

    @AssistedFactory
    interface Factory : SendAmountComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SendAmountComponentParams.AmountParams,
        ): DefaultSendAmountComponent
    }
}