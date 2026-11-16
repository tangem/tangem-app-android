package com.tangem.lib.auth.social.internal

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.lib.auth.api.models.request.DeviceMetadata
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SocialWalletRegistrationApiRequestTest {

    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(SocialWalletRegistrationApiRequest::class.java)
    private val bodyAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN request WHEN serialized THEN kmsKeyId matches the backend contract`(model: SerializeModel) {
        // Act
        val body = bodyAdapter.fromJson(adapter.toJson(createRequest(kmsKeyId = model.kmsKeyId)))

        // Assert
        if (model.kmsKeyId == null) {
            assertThat(body).doesNotContainKey("kmsKeyId")
        } else {
            assertThat(body).containsEntry("kmsKeyId", model.kmsKeyId)
        }
    }

    private fun provideTestModels() = listOf(
        SerializeModel(kmsKeyId = "Recovery-Key/2026-11 v3"),
        SerializeModel(kmsKeyId = null),
    )

    private fun createRequest(kmsKeyId: String?) = SocialWalletRegistrationApiRequest(
        nonce = "bm9uY2Ux",
        walletId = "d2FsbGV0",
        walletSignature = "signature",
        walletSignatureSalt = "salt",
        providerIdToken = "provider-id-token",
        encryptedRecoveryKey = "sealed",
        kmsKeyId = kmsKeyId,
        attestationToken = null,
        metadata = DeviceMetadata(
            deviceModel = "Pixel 7",
            os = "android",
            osVersion = "16",
            appVersion = "5.40.0",
            userAgent = "Tangem/5.40.0",
            locale = "en-US",
            timezone = "UTC",
        ),
    )

    data class SerializeModel(val kmsKeyId: String?)
}