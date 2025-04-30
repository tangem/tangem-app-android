package com.tangem.data.visa.converter

import com.tangem.datasource.api.visa.models.response.CardActivationRemoteStateResponse
import com.tangem.domain.visa.model.VisaActivationOrderInfo
import com.tangem.domain.visa.model.VisaActivationRemoteState
import com.tangem.utils.converter.Converter
import kotlinx.coroutines.flow.MutableStateFlow

private const val PIN_CODE_VALIDATION_ERROR = 1000

object VisaActivationStatusConverterWithState :
    Converter<CardActivationRemoteStateResponse, VisaActivationRemoteState> {

    private val lastUpdatedAt = MutableStateFlow<String?>(null)

    override fun convert(value: CardActivationRemoteStateResponse): VisaActivationRemoteState {
        // handle pin code error
        // either we entered pin code before with an error (WasError) or after receiving an error (InProgress)
        // TODO [REDACTED_TASK_KEY]
        val result = value.result
        if (result.status == Status.PinCodeRequired.stringValue && result.stepChangeCode == PIN_CODE_VALIDATION_ERROR) {
            return if (lastUpdatedAt.value != result.updatedAt) {
                lastUpdatedAt.value = result.updatedAt

                VisaActivationRemoteState.AwaitingPinCode(
                    activationOrderInfo = result.activationOrder!!.convert(),
                    status = VisaActivationRemoteState.AwaitingPinCode.Status.WasError,
                )
            } else {
                VisaActivationRemoteState.AwaitingPinCode(
                    activationOrderInfo = result.activationOrder!!.convert(),
                    status = VisaActivationRemoteState.AwaitingPinCode.Status.InProgress,
                )
            }
        }

        val status = Status.entries.find { it.stringValue == result.status } ?: error(
            "Unknown status: ${result.status}",
        )

        return when (status) {
            Status.Activated -> VisaActivationRemoteState.Activated
            Status.Failed -> VisaActivationRemoteState.Failed
            Status.BlockedForActivation -> VisaActivationRemoteState.BlockedForActivation
            Status.CardWalletSignatureRequired ->
                VisaActivationRemoteState.CardWalletSignatureRequired(
                    activationOrderInfo = result.activationOrder!!.convert(),
                )
            Status.CustomerWalletSignatureRequired ->
                VisaActivationRemoteState.CustomerWalletSignatureRequired(
                    activationOrderInfo = result.activationOrder!!.convert(),
                )
            Status.PaymentAccountDeploying -> VisaActivationRemoteState.PaymentAccountDeploying
            Status.PinCodeRequired -> VisaActivationRemoteState.AwaitingPinCode(
                activationOrderInfo = result.activationOrder!!.convert(),
                status = VisaActivationRemoteState.AwaitingPinCode.Status.WaitingForPinCode,
            )
            Status.WaitingForActivation -> VisaActivationRemoteState.WaitingForActivationFinishing
        }
    }

    private fun CardActivationRemoteStateResponse.ActivationOrder.convert(): VisaActivationOrderInfo {
        return VisaActivationOrderInfo(
            orderId = id,
            customerId = customerId,
            customerWalletAddress = customerWalletAddress,
            cardWalletAddress = cardWalletAddress.takeIf { it.isNotBlank() },
        )
    }

    private enum class Status(val stringValue: String) {
        Activated("activated"),
        Failed("failed"),
        BlockedForActivation("blocked_for_activation"),
        CardWalletSignatureRequired("card_wallet_signature_required"),
        CustomerWalletSignatureRequired("customer_wallet_signature_required"),
        PaymentAccountDeploying("payment_account_deploying"),
        PinCodeRequired("pin_code_required"),
        WaitingForActivation("waiting_for_activation"),
    }
}