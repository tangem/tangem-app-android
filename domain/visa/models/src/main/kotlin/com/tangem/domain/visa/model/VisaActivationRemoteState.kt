package com.tangem.domain.visa.model

import com.squareup.moshi.*
import com.tangem.domain.visa.model.VisaActivationRemoteState.*
import kotlinx.serialization.Serializable

@Serializable
sealed class VisaActivationRemoteState {

    @Serializable
    data class CardWalletSignatureRequired(
        val activationOrderInfo: VisaActivationOrderInfo,
    ) : VisaActivationRemoteState()

    @Serializable
    data class CustomerWalletSignatureRequired(
        val activationOrderInfo: VisaActivationOrderInfo,
    ) : VisaActivationRemoteState()

    @Serializable
    data object PaymentAccountDeploying : VisaActivationRemoteState()

    @Serializable
    data class AwaitingPinCode(
        val activationOrderInfo: VisaActivationOrderInfo,
        val status: Status,
    ) : VisaActivationRemoteState() {
        enum class Status {
            WaitingForPinCode, InProgress, WasError
        }
    }

    @Serializable
    data object WaitingForActivationFinishing : VisaActivationRemoteState()

    @Serializable
    data object Activated : VisaActivationRemoteState()

    @Serializable
    data object BlockedForActivation : VisaActivationRemoteState()

    @Serializable
    data object Failed : VisaActivationRemoteState()

    companion object {
        val jsonAdapter: VisaActivationRemoteState_JsonAdapter = VisaActivationRemoteState_JsonAdapter()
    }
}

@Suppress("ClassNaming")
class VisaActivationRemoteState_Json(
    @Json(name = "type") val type: VisaActivationRemoteState_Type,
    @Json(name = "activationOrderInfo") val activationOrderInfo: VisaActivationOrderInfo? = null,
    @Json(name = "awaiting_pin_code_status") val awaitingPinCodeStatus: AwaitingPinCodeStatus_Type? = null,
)

@Suppress("ClassNaming")
// do not rename
@JsonClass(generateAdapter = false)
enum class VisaActivationRemoteState_Type {
    CardWalletSignatureRequired,
    CustomerWalletSignatureRequired,
    PaymentAccountDeploying,
    WaitingPinCode,
    WaitingForActivationFinishing,
    Activated,
    BlockedForActivation,
    Failed,
}

@Suppress("ClassNaming")
@JsonClass(generateAdapter = false)
enum class AwaitingPinCodeStatus_Type {
    WaitingForPinCode, InProgress, WasError
}

@Suppress("ClassNaming")
class VisaActivationRemoteState_JsonAdapter {

    @FromJson
    fun fromJson(value: VisaActivationRemoteState_Json): VisaActivationRemoteState {
        return when (value.type) {
            VisaActivationRemoteState_Type.CardWalletSignatureRequired ->
                CardWalletSignatureRequired(value.activationOrderInfo!!)
            VisaActivationRemoteState_Type.CustomerWalletSignatureRequired ->
                CustomerWalletSignatureRequired(value.activationOrderInfo!!)
            VisaActivationRemoteState_Type.PaymentAccountDeploying -> PaymentAccountDeploying
            VisaActivationRemoteState_Type.WaitingPinCode ->
                AwaitingPinCode(
                    activationOrderInfo = value.activationOrderInfo!!,
                    status = when (value.awaitingPinCodeStatus!!) {
                        AwaitingPinCodeStatus_Type.WaitingForPinCode ->
                            AwaitingPinCode.Status.WaitingForPinCode
                        AwaitingPinCodeStatus_Type.InProgress ->
                            AwaitingPinCode.Status.InProgress
                        AwaitingPinCodeStatus_Type.WasError ->
                            AwaitingPinCode.Status.WasError
                    },
                )
            VisaActivationRemoteState_Type.WaitingForActivationFinishing ->
                WaitingForActivationFinishing
            VisaActivationRemoteState_Type.Activated -> Activated
            VisaActivationRemoteState_Type.BlockedForActivation -> BlockedForActivation
            VisaActivationRemoteState_Type.Failed -> Failed
        }
    }

    @ToJson
    fun toJson(value: VisaActivationRemoteState): VisaActivationRemoteState_Json {
        return when (value) {
            is CardWalletSignatureRequired ->
                VisaActivationRemoteState_Json(
                    type = VisaActivationRemoteState_Type.CardWalletSignatureRequired,
                    activationOrderInfo = value.activationOrderInfo,
                )
            is CustomerWalletSignatureRequired ->
                VisaActivationRemoteState_Json(
                    type = VisaActivationRemoteState_Type.CustomerWalletSignatureRequired,
                    activationOrderInfo = value.activationOrderInfo,
                )
            is PaymentAccountDeploying ->
                VisaActivationRemoteState_Json(VisaActivationRemoteState_Type.PaymentAccountDeploying)
            is AwaitingPinCode ->
                VisaActivationRemoteState_Json(
                    VisaActivationRemoteState_Type.WaitingPinCode,
                    activationOrderInfo = value.activationOrderInfo,
                    awaitingPinCodeStatus = when (value.status) {
                        AwaitingPinCode.Status.WaitingForPinCode ->
                            AwaitingPinCodeStatus_Type.WaitingForPinCode
                        AwaitingPinCode.Status.InProgress ->
                            AwaitingPinCodeStatus_Type.InProgress
                        AwaitingPinCode.Status.WasError ->
                            AwaitingPinCodeStatus_Type.WasError
                    },
                )
            is WaitingForActivationFinishing ->
                VisaActivationRemoteState_Json(VisaActivationRemoteState_Type.WaitingForActivationFinishing)
            is Activated -> VisaActivationRemoteState_Json(
                VisaActivationRemoteState_Type.Activated,
            )
            is BlockedForActivation ->
                VisaActivationRemoteState_Json(VisaActivationRemoteState_Type.BlockedForActivation)
            is Failed -> VisaActivationRemoteState_Json(VisaActivationRemoteState_Type.Failed)
        }
    }
}