package com.tangem.features.approval.impl.model

import androidx.compose.runtime.Stable
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.approval.api.SelectApprovalTypeComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Model for [SelectApprovalTypeComponent].
 *
 * Keeps the currently selected [ApproveType] and exposes intents to change it, open the
 * learn-more URL, confirm the selection, and cancel. Unlike [GiveApprovalModel] this model
 * does NOT load fees or submit any transaction — confirmation simply notifies the caller
 * via the params callback with the selected [ApproveType].
 */
@Stable
@ModelScoped
internal class SelectApprovalTypeModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: SelectApprovalTypeComponent.Params = paramsContainer.require()

    val uiState: StateFlow<SelectApprovalTypeUM>
        field = MutableStateFlow(
            SelectApprovalTypeUM(
                approveType = params.initialApproveType,
                subtitle = params.amountFooter,
            ),
        )

    fun onChangeApproveType(approveType: ApproveType) {
        if (uiState.value.approveType == approveType) return
        uiState.update { it.copy(approveType = approveType) }
    }

    fun onConfirmClick() {
        params.callback.onApproveTypeSelected(uiState.value.approveType)
    }

    fun onCancelClick() {
        params.callback.onCancelClick()
    }
}