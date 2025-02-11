package com.tangem.domain.visa.model

import com.squareup.moshi.*
import kotlinx.serialization.Serializable

@Serializable
sealed class VisaActivationRemoteState {

    @Serializable
    data class CardWalletSignatureRequired(
        val request: VisaCardWalletDataToSignRequest,
    ) : VisaActivationRemoteState()

    @Serializable
    data class CustomerWalletSignatureRequired(
        val activationOrderInfo: VisaActivationOrderInfo,
    ) : VisaActivationRemoteState()

    @Serializable
    data object PaymentAccountDeploying : VisaActivationRemoteState()

    @Serializable
    data class WaitingPinCode(
        val activationOrderInfo: VisaActivationOrderInfo,
    ) : VisaActivationRemoteState()

    @Serializable
    data object WaitingForActivationFinishing : VisaActivationRemoteState()

    @Serializable
    data object Activated : VisaActivationRemoteState()

    @Serializable
    data object BlockedForActivation : VisaActivationRemoteState()

    companion object {
        val jsonAdapter: VisaActivationRemoteState_JsonAdapter = VisaActivationRemoteState_JsonAdapter()
    }
}

@Suppress("ClassNaming")
class VisaActivationRemoteState_Json(
    @Json(name = "type") val type: VisaActivationRemoteState_Type,
    @Json(name = "requestCardWallet") val requestCardWallet: VisaCardWalletDataToSignRequest? = null,
    @Json(name = "activationOrderInfo") val activationOrderInfo: VisaActivationOrderInfo? = null,
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
}

@Suppress("ClassNaming")
class VisaActivationRemoteState_JsonAdapter {

    @FromJson
    fun fromJson(value: VisaActivationRemoteState_Json): VisaActivationRemoteState {
        return when (value.type) {
            VisaActivationRemoteState_Type.CardWalletSignatureRequired ->
                VisaActivationRemoteState.CardWalletSignatureRequired(value.requestCardWallet!!)
            VisaActivationRemoteState_Type.CustomerWalletSignatureRequired ->
                VisaActivationRemoteState.CustomerWalletSignatureRequired(value.activationOrderInfo!!)
            VisaActivationRemoteState_Type.PaymentAccountDeploying -> VisaActivationRemoteState.PaymentAccountDeploying
            VisaActivationRemoteState_Type.WaitingPinCode ->
                VisaActivationRemoteState.WaitingPinCode(value.activationOrderInfo!!)
            VisaActivationRemoteState_Type.WaitingForActivationFinishing ->
                VisaActivationRemoteState.WaitingForActivationFinishing
            VisaActivationRemoteState_Type.Activated -> VisaActivationRemoteState.Activated
            VisaActivationRemoteState_Type.BlockedForActivation -> VisaActivationRemoteState.BlockedForActivation
        }
    }

    @ToJson
    fun toJson(value: VisaActivationRemoteState): VisaActivationRemoteState_Json {
        return when (value) {
            is VisaActivationRemoteState.CardWalletSignatureRequired ->
                VisaActivationRemoteState_Json(
                    type = VisaActivationRemoteState_Type.CardWalletSignatureRequired,
                    requestCardWallet = value.request,
                )
            is VisaActivationRemoteState.CustomerWalletSignatureRequired ->
                VisaActivationRemoteState_Json(
                    type = VisaActivationRemoteState_Type.CustomerWalletSignatureRequired,
                    activationOrderInfo = value.activationOrderInfo,
                )
            is VisaActivationRemoteState.PaymentAccountDeploying ->
                VisaActivationRemoteState_Json(VisaActivationRemoteState_Type.PaymentAccountDeploying)
            is VisaActivationRemoteState.WaitingPinCode ->
                VisaActivationRemoteState_Json(
                    VisaActivationRemoteState_Type.WaitingPinCode,
                    activationOrderInfo = value.activationOrderInfo,
                )
            is VisaActivationRemoteState.WaitingForActivationFinishing ->
                VisaActivationRemoteState_Json(VisaActivationRemoteState_Type.WaitingForActivationFinishing)
            is VisaActivationRemoteState.Activated -> VisaActivationRemoteState_Json(
                VisaActivationRemoteState_Type.Activated,
            )
            is VisaActivationRemoteState.BlockedForActivation ->
                VisaActivationRemoteState_Json(VisaActivationRemoteState_Type.BlockedForActivation)
        }
    }
}