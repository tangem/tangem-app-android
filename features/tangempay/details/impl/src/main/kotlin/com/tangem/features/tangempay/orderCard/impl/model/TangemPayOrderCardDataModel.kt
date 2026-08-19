package com.tangem.features.tangempay.orderCard.impl.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.domain.pay.model.PlasticCardOrder
import com.tangem.domain.pay.model.ShippingAddress
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.IssuePlasticCardUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.orderCard.impl.TangemPayOrderCardDataComponent
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM.FieldUM
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM.Form
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val TAG = "TangemPayOrderCardDataModel"

@Stable
@ModelScoped
internal class TangemPayOrderCardDataModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val onboardingRepository: OnboardingRepository,
    private val issuePlasticCard: IssuePlasticCardUseCase,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<TangemPayOrderCardDataComponent.Params>()
    private val loadDataJobHolder = JobHolder()
    private val submitJobHolder = JobHolder()

    val state: StateFlow<TangemPayOrderCardDataScreenUM>
        field = MutableStateFlow<TangemPayOrderCardDataScreenUM>(createLoadingState())

    init {
        loadData()
    }

    private fun createLoadingState() = TangemPayOrderCardDataScreenUM.Loading(
        onBackClick = ::onBackClick,
        onCloseClick = params.onClose,
    )

    private fun createErrorState() = TangemPayOrderCardDataScreenUM.Error(
        onBackClick = ::onBackClick,
        onCloseClick = params.onClose,
        onRetry = ::loadData,
    )

    private fun createFormState(country: String, email: String, mask: String) = Form(
        onBackClick = ::onBackClick,
        onCloseClick = params.onClose,
        country = country,
        email = email,
        phoneMask = mask,
        embossName = emptyField(OrderFormField.EmbossName),
        firstName = emptyField(OrderFormField.FirstName),
        lastName = emptyField(OrderFormField.LastName),
        region = emptyField(OrderFormField.Region),
        city = emptyField(OrderFormField.City),
        addressLine1 = emptyField(OrderFormField.Line1),
        addressLine2 = emptyField(OrderFormField.Line2),
        postalCode = emptyField(OrderFormField.PostalCode),
        phone = FieldUM(
            value = "",
            error = null,
            isRequired = true,
            onValueChange = ::onPhoneChange,
            onFocusChange = ::onPhoneFocusChange,
        ),
        isOrderEnabled = false,
        isSubmitting = false,
        onOrderClick = ::onOrderClick,
    )

    private fun emptyField(field: OrderFormField) = FieldUM(
        value = "",
        error = null,
        isRequired = field.isRequired,
        onValueChange = { onFieldChange(field, it) },
        onFocusChange = { onFieldFocusChange(field, it) },
    )

    private fun loadData() {
        state.value = createLoadingState()
        modelScope.launch {
            val info = onboardingRepository.getCustomerInfo(params.userWalletId).getOrNull()
            val mask = info?.phoneMask.orEmpty().takeIf(PhoneMaskFormatter::isUsable).orEmpty()
            val email = info?.email.orEmpty()
            val country = info?.country.orEmpty().trim()
            state.value = if (email.isNotBlank() && country.isNotBlank()) {
                createFormState(country = country, email = email, mask = mask)
            } else {
                createErrorState()
            }
        }.saveIn(loadDataJobHolder)
    }

    private fun onFieldChange(field: OrderFormField, value: String) {
        updateForm { form -> form.updateField(field) { copy(value = value, error = null) } }
    }

    private fun onFieldFocusChange(field: OrderFormField, isFocused: Boolean) {
        if (isFocused) return
        updateForm { form -> form.updateField(field) { copy(error = form.fieldError(field)) } }
    }

    private fun onPhoneChange(raw: String) {
        updateForm { form ->
            form.copy(
                phone = form.phone.copy(
                    value = PhoneMaskFormatter.sanitize(raw = raw, mask = form.phoneMask),
                    error = null,
                ),
            )
        }
    }

    private fun onPhoneFocusChange(isFocused: Boolean) {
        if (isFocused) return
        updateForm { form -> form.copy(phone = form.phone.copy(error = form.phoneError())) }
    }

    private fun onOrderClick() {
        val form = state.value as? Form ?: return
        if (!form.isOrderEnabled || form.isSubmitting) return

        submitOrder(
            order = form.toPlasticCardOrder(),
            email = form.email,
            idempotencyKey = UUID.randomUUID().toString(),
        )
    }

    private fun submitOrder(order: PlasticCardOrder, email: String, idempotencyKey: String) {
        setSubmitting(isSubmitting = true)
        modelScope.launch {
            issuePlasticCard(
                userWalletId = params.userWalletId,
                plasticCardOrder = order,
                idempotencyKey = idempotencyKey,
            ).fold(
                ifLeft = { error ->
                    setSubmitting(isSubmitting = false)
                    TangemLogger.withTag(TAG).e("Plastic card order was not accepted: $error")
                    uiMessageSender.send(
                        createSubmitFailedMessage(error) { submitOrder(order, email, idempotencyKey) },
                    )
                },
                ifRight = { createdOrder ->
                    setSubmitting(isSubmitting = false)
                    params.onOrderAccepted(email, createdOrder.productInstanceId)
                },
            )
        }.saveIn(submitJobHolder)
    }

    private fun createSubmitFailedMessage(error: VisaApiError, onRetry: () -> Unit): BottomSheetMessage {
        return when (error) {
            VisaApiError.CardIssueInvalidShippingAddress -> TangemPayMessagesFactory.createSubmitRejectedMessage(
                title = resourceReference(R.string.tangempay_order_card_error_invalid_address),
            )
            VisaApiError.CardIssueInsufficientBalance -> TangemPayMessagesFactory.createSubmitRejectedMessage(
                title = resourceReference(R.string.tangempay_order_card_error_insufficient_balance),
                onCloseClick = params.onClose,
            )
            VisaApiError.CardIssueActiveOrderExists -> TangemPayMessagesFactory.createSubmitRejectedMessage(
                title = resourceReference(R.string.tangempay_order_card_error_active_order),
                onCloseClick = params.onClose,
            )
            VisaApiError.CardIssueOfferNotAvailable -> TangemPayMessagesFactory.createSubmitRejectedMessage(
                title = resourceReference(R.string.tangempay_order_card_error_offer_unavailable),
                onCloseClick = params.onClose,
            )
            else -> TangemPayMessagesFactory.createOrderFailedMessage(onRetry.takeIf { error.isRetryable() })
        }
    }

    private fun onBackClick() {
        router.pop()
    }

    private fun setSubmitting(isSubmitting: Boolean) {
        state.update { current -> if (current is Form) current.copy(isSubmitting = isSubmitting) else current }
    }

    private fun updateForm(transform: (Form) -> Form) {
        state.update { current ->
            if (current is Form) {
                val updated = transform(current)
                updated.copy(isOrderEnabled = updated.isFormValid())
            } else {
                current
            }
        }
    }
}

private fun Form.toPlasticCardOrder() = PlasticCardOrder(
    embossName = embossName.value.trim(),
    shippingAddress = ShippingAddress(
        firstName = firstName.value.trim(),
        lastName = lastName.value.trim(),
        region = region.value.trim(),
        city = city.value.trim(),
        line1 = addressLine1.value.trim(),
        line2 = addressLine2.value.trim().ifBlank { null },
        postalCode = postalCode.value.trim(),
        phone = PhoneMaskFormatter.toE164(phone.value, phoneMask),
    ),
)