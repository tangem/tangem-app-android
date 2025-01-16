package com.tangem.domain.visa.model

import com.squareup.moshi.Json
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import kotlinx.serialization.Serializable

@Serializable
sealed class VisaCardActivationStatus {

    @Serializable
    data class Activated(
        @Json(name = "visaAuthTokens") val visaAuthTokens: VisaAuthTokens,
    ) : VisaCardActivationStatus()

    @Serializable
    data class ActivationStarted(
        @Json(name = "activationInput") val activationInput: VisaActivationInput,
        @Json(name = "authTokens") val authTokens: VisaAuthTokens,
        @Json(name = "remoteState") val remoteState: VisaActivationRemoteState,
    ) : VisaCardActivationStatus()

    @Serializable
    data class NotStartedActivation(
        @Json(name = "activationInput") val activationInput: VisaActivationInput,
    ) : VisaCardActivationStatus()

    @Serializable
    data object Blocked : VisaCardActivationStatus()

    companion object {
        val jsonAdapter: PolymorphicJsonAdapterFactory<VisaCardActivationStatus>
            get() = PolymorphicJsonAdapterFactory.of(VisaCardActivationStatus::class.java, "type")
                .withSubtype(Activated::class.java, "Activated")
                .withSubtype(ActivationStarted::class.java, "ActivationStarted")
                .withSubtype(NotStartedActivation::class.java, "NotStartedActivation")
                .withDefaultValue(Blocked)
    }
}