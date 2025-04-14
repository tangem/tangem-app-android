package com.tangem.data.visa.converter

import com.tangem.datasource.api.visa.models.response.CardActivationRemoteStateResponse
import com.tangem.domain.visa.model.VisaActivationOrderInfo
import com.tangem.domain.visa.model.VisaActivationRemoteState
import com.tangem.utils.converter.Converter
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisaActivationStatusConverter @Inject constructor() :
    Converter<CardActivationRemoteStateResponse, VisaActivationRemoteState> {

    private val lastUpdatedAt = MutableStateFlow<String?>(null)

    override fun convert(value: CardActivationRemoteStateResponse): VisaActivationRemoteState {
        // handle pin code error
        // either we entered pin code before with an error (WasError) or after receiving an error (InProgress)
        if (
            value.status == Status.AwaitingPin.stringValue &&
            value.stepChangeCode != null &&
            value.stepChangeCode == PIN_CODE_VALIDATION_ERROR
        ) {
            return if (lastUpdatedAt.value != value.updatedAt) {
                lastUpdatedAt.value == value.updatedAt

                VisaActivationRemoteState.AwaitingPinCode(
                    activationOrderInfo = value.activationOrder!!.convert(),
                    status = VisaActivationRemoteState.AwaitingPinCode.Status.WasError,
                )
            } else {
                VisaActivationRemoteState.AwaitingPinCode(
                    activationOrderInfo = value.activationOrder!!.convert(),
                    status = VisaActivationRemoteState.AwaitingPinCode.Status.InProgress,
                )
            }
        }

        // TODO Will be implemented in the future
        return VisaActivationRemoteState.Activated
    }

    private fun CardActivationRemoteStateResponse.ActivationOrder.convert(): VisaActivationOrderInfo {
        return VisaActivationOrderInfo(
            orderId = id,
            customerId = customerId,
            customerWalletAddress = customerWalletAddress,
        )
    }

    private enum class Status(val stringValue: String) {
        AwaitingPin("AWAITING_PIN"),
        // TODO complete statuses list when backend is ready
    }

    private companion object {
        const val PIN_CODE_VALIDATION_ERROR = 1000
    }
}