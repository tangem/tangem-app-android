package com.tangem.features.tangempay.card.activation

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.pay.activationImageUrl
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.CARD_ACTIVATION_LAST_DIGITS_LENGTH
import com.tangem.domain.pay.model.CardActivationOrder
import com.tangem.domain.pay.usecase.ActivatePlasticCardUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
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

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayCardActivationModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analytics: AnalyticsEventHandler,
    private val router: Router,
    private val activatePlasticCardUseCase: ActivatePlasticCardUseCase,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val uiMessageSender: UiMessageSender,
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
                cardImageUrl = params.card.activationImageUrl,
                hint = resourceReference(R.string.tangempay_card_activation_description),
                isHintError = false,
                isLoading = false,
                onLastDigitsChange = ::onLastDigitsChange,
                onContinueClick = ::onContinueClick,
                onCloseClick = router::pop,
            ),
        )

    init {
        analytics.send(TangemPayAnalyticsEvents.Plastic.CardActivationScreenOpened())
    }

    private fun onLastDigitsChange(input: String) {
        if (uiState.value.isLoading) return

        val previousDigits = uiState.value.lastDigits
        val digits = input.filter(Char::isDigit).take(CARD_ACTIVATION_LAST_DIGITS_LENGTH)
        if (digits != previousDigits) submitIdempotencyKey = null

        val isJustCompleted = digits.length == CARD_ACTIVATION_LAST_DIGITS_LENGTH &&
            previousDigits.length < CARD_ACTIVATION_LAST_DIGITS_LENGTH
        if (isJustCompleted) analytics.send(TangemPayAnalyticsEvents.Plastic.CardLast4DigitsEntered())

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

        analytics.send(TangemPayAnalyticsEvents.Plastic.ActivationContinueClicked())

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
                .onRight {
                    analytics.send(TangemPayAnalyticsEvents.Plastic.CardActivationSuccess())
                    router.pop()
                }
                .onLeft { error -> onSubmitFailed(error) }
        }.saveIn(submitJobHolder)
    }

    private suspend fun onSubmitFailed(error: VisaApiError) {
        setSubmitFinished()
        when (error) {
            VisaApiError.CardActivationInvalidCardData -> showInlineError(
                message = resourceReference(R.string.tangempay_card_activation_error),
            )
            VisaApiError.CardActivationCardNotPhysical -> showRejectionSheet(
                title = resourceReference(R.string.tangempay_card_activation_error_not_physical),
                onClose = { router.popTo(TangemPayAccountDetailsInnerRoute.AccountDetails) },
            )
            VisaApiError.CardActivationCardAlreadyActive -> {
                paymentAccountStatusFetcher.invoke(params.userWalletId)
                showRejectionSheet(
                    title = resourceReference(R.string.tangempay_card_activation_error_already_active),
                    onClose = { router.pop() },
                )
            }
            VisaApiError.CardActivationCardNotReadyForActivation -> showRejectionSheet(
                title = resourceReference(R.string.tangempay_card_activation_error_not_ready),
                onClose = { router.pop() },
            )
            VisaApiError.CardActivationActiveOrderExists -> {
                paymentAccountStatusFetcher.invoke(params.userWalletId)
                showRejectionSheet(
                    title = resourceReference(R.string.tangempay_card_activation_error_in_progress),
                    onClose = { router.pop() },
                )
            }
            else -> showRejectionSheet(
                title = resourceReference(CoreUiR.string.common_something_went_wrong),
            )
        }
    }

    private fun showRejectionSheet(title: TextReference, onClose: () -> Unit = {}) {
        uiMessageSender.send(
            TangemPayMessagesFactory.createSubmitRejectedMessage(title = title, onCloseClick = onClose),
        )
    }

    private fun showInlineError(message: TextReference) {
        analytics.send(TangemPayAnalyticsEvents.Plastic.Last4DigitsValidationErrorShowed())
        uiState.update { current ->
            current.copy(lastDigits = "", hint = message, isHintError = true)
        }
    }

    private fun setSubmitFinished() {
        uiState.update { current -> current.copy(isLoading = false) }
    }
}