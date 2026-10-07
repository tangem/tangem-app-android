package com.tangem.lib.auth.api.models.request

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletUnregisterRequestTest {

    private val adapter = Moshi.Builder().build().adapter(WalletUnregisterRequest::class.java)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN request WHEN serialized THEN eraseData matches the backend contract`(model: SerializeModel) {
        // Act
        val json = adapter.toJson(model.request)

        // Assert
        assertThat(json).isEqualTo(model.expectedJson)
    }

    private fun provideTestModels() = listOf(
        SerializeModel(
            request = WalletUnregisterRequest(walletId = WALLET_ID, shouldDeleteSocialRecovery = true),
            expectedJson = """{"walletId":"$WALLET_ID","eraseData":true}""",
        ),
        SerializeModel(
            request = WalletUnregisterRequest(walletId = WALLET_ID),
            expectedJson = """{"walletId":"$WALLET_ID"}""",
        ),
    )

    data class SerializeModel(val request: WalletUnregisterRequest, val expectedJson: String)

    private companion object {
        const val WALLET_ID = "d2FsbGV0"
    }
}