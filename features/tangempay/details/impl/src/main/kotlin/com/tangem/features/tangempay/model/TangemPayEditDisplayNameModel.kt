package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayEditDisplayNameUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayEditDisplayNameModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params: TangemPayDetailsContainerComponent.Params = paramsContainer.require()

    private val originalDisplayName = params.config.displayName?.value.orEmpty()

    val uiState: StateFlow<TangemPayEditDisplayNameUM>
        field = MutableStateFlow(
            TangemPayEditDisplayNameUM(
                editingValue = originalDisplayName,
                isLoading = false,
                onValueChanged = ::onValueChanged,
                onDoneClick = ::onDoneClick,
                onDismiss = ::onDismiss,
            ),
        )

    private fun onValueChanged(value: String) {
        if (value.length <= CardDisplayName.MAX_LENGTH) {
            uiState.update { it.copy(editingValue = value) }
        }
    }

    private fun onDoneClick() {
        val currentValue = uiState.value.editingValue
        if (currentValue.trim() == originalDisplayName.trim()) {
            router.pop()
            return
        }
        CardDisplayName(currentValue)
            .onRight { cardDisplayName ->
                uiState.update { it.copy(isLoading = true) }
                modelScope.launch {
                    cardDetailsRepository.updateCardDisplayName(params.userWalletId, cardDisplayName)
                        .onRight { router.pop() }
                        .onLeft {
                            uiState.update { state -> state.copy(isLoading = false) }
                            showError(
                                titleRes = R.string.tangem_pay_card_details_unable_to_rename_card_title,
                                messageRes = R.string.tangempay_card_details_unable_to_rename_card_description,
                            )
                        }
                }
            }
            .onLeft {
                showError(
                    titleRes = R.string.tangempay_card_details_rename_card_invalid_title,
                    messageRes = R.string.tangempay_card_details_rename_card_invalid_description,
                )
            }
    }

    private fun showError(titleRes: Int, messageRes: Int) {
        uiMessageSender.send(
            DialogMessage(title = TextReference.Res(titleRes), message = TextReference.Res(messageRes)),
        )
    }

    private fun onDismiss() {
        router.pop()
    }
}