package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.hasCardWithId
import com.tangem.domain.models.account.requireCardWithId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.usecase.UpdateTangemPayCardNameUseCase
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.components.TangemPayCardScopedParams
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.features.tangempay.entity.TangemPayEditDisplayNameUM
import com.tangem.features.tangempay.model.controller.TangemPayCardDetailsController
import com.tangem.features.tangempay.utils.requireLoaded
import com.tangem.features.tangempay.utils.userWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayEditDisplayNameModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val updateCardNameUseCase: UpdateTangemPayCardNameUseCase,
    private val uiMessageSender: UiMessageSender,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    private val featureToggles: TangemPayFeatureToggles,
    cardDetailsControllerFactory: TangemPayCardDetailsController.Factory,
) : Model() {

    private val params: TangemPayCardScopedParams = paramsContainer.require()

    private val card = params.initialStatus.requireLoaded().requireCardWithId(params.cardId)
    private val originalDisplayName = card.displayName?.value.orEmpty()

    private val cardDetailsController = cardDetailsControllerFactory.create(
        scope = modelScope,
        initialCard = card,
        userWalletId = params.initialStatus.userWalletId,
        config = TangemPayCardDetailsController.Config(
            isEditingNameEnabled = false,
            shouldShowCardDetailsButtonOnCard = false,
        ),
        onEditNameClick = {},
    )

    val cardDetailsState: StateFlow<TangemPayCardDetailsUM> = cardDetailsController.uiState

    val uiState: StateFlow<TangemPayEditDisplayNameUM>
        field = MutableStateFlow(
            TangemPayEditDisplayNameUM(
                editingValue = TextFieldValue(
                    text = originalDisplayName,
                    selection = TextRange(originalDisplayName.length),
                ),
                isLoading = false,
                isDoneEnabled = true,
                onValueChanged = ::onValueChanged,
                onDoneClick = ::onDoneClick,
                onDismiss = ::onDismiss,
            ),
        )

    init {
        subscribeToCardNameChanges(card.id, params.initialStatus.userWalletId)
    }

    fun isRedesignEnabled() = featureToggles.isRedesignEnabled

    private fun subscribeToCardNameChanges(cardId: String, userWalletId: UserWalletId) {
        paymentAccountStatusSupplier.invoke(userWalletId)
            .onEach { state ->
                val status = state.value
                if (status is PaymentAccountStatusValue.Loaded &&
                    status.source == StatusSource.ACTUAL &&
                    status.hasCardWithId(cardId)
                ) {
                    val card = status.requireCardWithId(cardId)
                    val displayName = card.displayName ?: return@onEach

                    uiState.update { uiState ->
                        uiState.copy(
                            editingValue = TextFieldValue(
                                text = displayName.value,
                                selection = TextRange(displayName.value.length),
                            ),
                        )
                    }
                }
            }
            .launchIn(modelScope)
    }

    private fun onValueChanged(value: TextFieldValue) {
        val displayName = CardDisplayName(value.text)
        val isAvailableForConfirm = displayName.isRight()
        uiState.update { it.copy(editingValue = value, isDoneEnabled = isAvailableForConfirm) }
    }

    private fun onDoneClick() {
        val currentValue = uiState.value.editingValue.text
        if (currentValue.trim() == originalDisplayName.trim()) {
            router.pop()
            return
        }
        CardDisplayName(currentValue)
            .onRight { cardDisplayName ->
                modelScope.launch {
                    uiState.update { it.copy(isLoading = true) }
                    updateCardNameUseCase(
                        cardId = card.id,
                        userWalletId = params.initialStatus.userWalletId,
                        displayName = cardDisplayName,
                    ).onRight {
                        router.pop()
                    }.onLeft {
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