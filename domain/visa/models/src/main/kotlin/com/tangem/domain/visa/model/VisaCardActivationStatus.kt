package com.tangem.domain.visa.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import kotlinx.serialization.Serializable

@Serializable
sealed class VisaCardActivationStatus {

    @Serializable
    data class Activated(
        val visaAuthTokens: VisaAuthTokens,
    ) : VisaCardActivationStatus()

    @Serializable
    data class ActivationStarted(
        val activationInput: VisaActivationInput,
        val authTokens: VisaAuthTokens,
        val remoteState: VisaActivationRemoteState,
    ) : VisaCardActivationStatus()

    @Serializable
    data class NotStartedActivation(
        val activationInput: VisaActivationInput,
    ) : VisaCardActivationStatus()

    @Serializable
    data object Blocked : VisaCardActivationStatus()

    @Serializable
    data object RefreshTokenExpired : VisaCardActivationStatus()

    companion object {
        val jsonAdapter: VisaCardActivationStatus_JsonAdapter = VisaCardActivationStatus_JsonAdapter()
    }
}

@Suppress("ClassNaming")
class VisaCardActivationStatus_Json(
    @Json(name = "type") val type: VisaCardActivationStatus_Type,
    @Json(name = "activationInput") val activationInput: VisaActivationInput? = null,
    @Json(name = "authTokens") val authTokens: VisaAuthTokens? = null,
    @Json(name = "remoteState") val remoteState: VisaActivationRemoteState? = null,
)

// do not rename instances
@Suppress("ClassNaming")
@JsonClass(generateAdapter = false)
enum class VisaCardActivationStatus_Type {
    Activated,
    ActivationStarted,
    NotStartedActivation,
    Blocked,
    RefreshTokenExpired,
}

@Suppress("ClassNaming")
class VisaCardActivationStatus_JsonAdapter {

    @FromJson
    fun fromJson(value: VisaCardActivationStatus_Json): VisaCardActivationStatus {
        return when (value.type) {
            VisaCardActivationStatus_Type.Activated -> VisaCardActivationStatus.Activated(value.authTokens!!)
            VisaCardActivationStatus_Type.ActivationStarted -> VisaCardActivationStatus.ActivationStarted(
                value.activationInput!!,
                value.authTokens!!,
                value.remoteState!!,
            )
            VisaCardActivationStatus_Type.NotStartedActivation -> VisaCardActivationStatus.NotStartedActivation(
                value.activationInput!!,
            )
            VisaCardActivationStatus_Type.Blocked -> VisaCardActivationStatus.Blocked
            VisaCardActivationStatus_Type.RefreshTokenExpired -> VisaCardActivationStatus.RefreshTokenExpired
        }
    }

    @ToJson
    fun toJson(value: VisaCardActivationStatus): VisaCardActivationStatus_Json {
        return when (value) {
            is VisaCardActivationStatus.Activated -> VisaCardActivationStatus_Json(
                VisaCardActivationStatus_Type.Activated,
                authTokens = value.visaAuthTokens,
            )
            is VisaCardActivationStatus.ActivationStarted -> VisaCardActivationStatus_Json(
                VisaCardActivationStatus_Type.ActivationStarted,
                value.activationInput,
                value.authTokens,
                value.remoteState,
            )
            is VisaCardActivationStatus.NotStartedActivation -> VisaCardActivationStatus_Json(
                VisaCardActivationStatus_Type.NotStartedActivation,
                activationInput = value.activationInput,
            )
            is VisaCardActivationStatus.Blocked -> VisaCardActivationStatus_Json(VisaCardActivationStatus_Type.Blocked)
            is VisaCardActivationStatus.RefreshTokenExpired -> VisaCardActivationStatus_Json(
                VisaCardActivationStatus_Type.RefreshTokenExpired,
            )
        }
    }
}