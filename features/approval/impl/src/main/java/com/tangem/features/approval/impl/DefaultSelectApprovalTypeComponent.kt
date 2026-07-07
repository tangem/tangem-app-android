package com.tangem.features.approval.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.approval.api.SelectApprovalTypeComponent
import com.tangem.features.approval.impl.model.SelectApprovalTypeModel
import com.tangem.features.approval.impl.ui.SelectApprovalTypeContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Default implementation of [SelectApprovalTypeComponent].
 *
 * Renders the same selection UI as the full [com.tangem.features.approval.api.GiveApprovalComponent]
 * but without the fee selector block and without dispatching the on-chain approval transaction.
 * Dismissing the bottom sheet (close button or external dismiss) is treated as a cancel.
 */
internal class DefaultSelectApprovalTypeComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SelectApprovalTypeComponent.Params,
) : SelectApprovalTypeComponent, AppComponentContext by appComponentContext {

    private val model: SelectApprovalTypeModel = getOrCreateModel(params = params)

    private val currency: String = params.cryptoCurrencyStatus.currency.symbol

    override fun dismiss() {
        params.callback.onCancelClick()
    }

    @Composable
    override fun BottomSheet() {
        val uiState by model.uiState.collectAsStateWithLifecycle()

        val config = remember {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = TangemBottomSheetConfigContent.Empty,
            )
        }

        TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
            config = config,
            containerColor = TangemTheme.colors.background.tertiary,
            titleText = resourceReference(R.string.give_permission_title),
            titleAction = TopAppBarButtonUM.Icon(
                iconRes = R.drawable.ic_close_new_20,
                onClicked = model::onCancelClick,
            ),
        ) {
            SelectApprovalTypeContent(
                currency = currency,
                uiState = uiState,
                onChangeApproveType = model::onChangeApproveType,
                onConfirmClick = model::onConfirmClick,
            )
        }
    }

    @AssistedFactory
    interface Factory : SelectApprovalTypeComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SelectApprovalTypeComponent.Params,
        ): DefaultSelectApprovalTypeComponent
    }
}