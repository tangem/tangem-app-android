package com.tangem.features.tangempay.card.activation

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.CARD_ACTIVATION_LAST_DIGITS_LENGTH
import com.tangem.domain.pay.model.CardActivationOrder
import com.tangem.domain.pay.usecase.ActivatePlasticCardUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

@Stable
@ModelScoped
internal class TangemPayCardActivationModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val activatePlasticCardUseCase: ActivatePlasticCardUseCase,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) : Model() {

    private val params: TangemPayCardActivationComponent.Params = paramsContainer.require()

    private val submitJobHolder = JobHolder()

    /**
     * Kept across retries of the same digits so a lost response cannot create a second activation order, and
     * reset as soon as the entered digits change — a different input is a different intent.
     */
    private var submitIdempotencyKey: String? = null

    val uiState: StateFlow<TangemPayCardActivationUM>
        field = MutableStateFlow(
            TangemPayCardActivationUM(
                lastDigits = "",
                cardImageUrl = params.cardImageUrl,
                hint = resourceReference(R.string.tangempay_card_activation_description),
                isHintError = false,
                isLoading = false,
                onLastDigitsChange = ::onLastDigitsChange,
                onContinueClick = ::onContinueClick,
                onCloseClick = router::pop,
            ),
        )

    private fun onLastDigitsChange(input: String) {
        if (uiState.value.isLoading) return

        val digits = input.filter(Char::isDigit).take(CARD_ACTIVATION_LAST_DIGITS_LENGTH)
        if (digits != uiState.value.lastDigits) submitIdempotencyKey = null

        uiState.update { state ->
            state.copy(
                lastDigits = digits,
                hint = resourceReference(R.string.tangempay_card_activation_description),
                isHintError = false,
            )
        }
    }

    private fun onContinueClick() {
        val state = uiState.value
        if (!state.isContinueEnabled || state.isLoading) return

        uiState.update { current ->
            current.copy(
                isLoading = true,
                hint = resourceReference(R.string.tangempay_card_activation_in_progress),
                isHintError = false,
            )
        }

        val idempotencyKey = submitIdempotencyKey ?: UUID.randomUUID().toString().also { submitIdempotencyKey = it }

        modelScope.launch {
            activatePlasticCardUseCase(
                userWalletId = params.userWalletId,
                activationOrder = CardActivationOrder(
                    productInstanceId = params.card.productInstanceId,
                    lastFourDigits = state.lastDigits,
                ),
                idempotencyKey = idempotencyKey,
            )
                .onRight { router.pop() }
                .onLeft { error -> onSubmitFailed(error) }
        }.saveIn(submitJobHolder)
    }

    private suspend fun onSubmitFailed(error: VisaApiError) {
        when (error) {
            VisaApiError.CardActivationInvalidCardData -> showError(
                message = resourceReference(R.string.tangempay_card_activation_error),
                shouldClearInput = true,
            )
            VisaApiError.CardActivationCardAlreadyActive,
            VisaApiError.CardActivationActiveOrderExists,
            -> {
                paymentAccountStatusFetcher.invoke(params.userWalletId)
                router.pop()
            }
            else -> showError(
                message = resourceReference(CoreUiR.string.common_unknown_error),
                shouldClearInput = false,
            )
        }
    }

    private fun showError(message: TextReference, shouldClearInput: Boolean) {
        uiState.update { current ->
            current.copy(
                lastDigits = if (shouldClearInput) "" else current.lastDigits,
                isLoading = false,
                hint = message,
                isHintError = true,
            )
        }
    }
}