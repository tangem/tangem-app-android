package com.tangem.data.transaction.convertes

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.data.transaction.api.models.SendIntentResponse
import com.tangem.data.transaction.api.models.SendIntentStatus
import com.tangem.domain.transaction.error.SendIntentError
import com.tangem.domain.transaction.models.SendIntent
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class SendIntentConverterTest {

    private val converter = SendIntentConverter()

    @Test
    fun `GIVEN created intent WHEN convert THEN every field is carried over`() {
        // Act
        val actual = converter.convert(createResponse())

        // Assert
        assertThat(actual).isEqualTo(Either.Right(createIntent()))
    }

    @Test
    fun `GIVEN blank memo and comment WHEN convert THEN they are absent`() {
        // Act
        val actual = converter.convert(createResponse(memo = " ", comment = ""))

        // Assert
        assertThat(actual).isEqualTo(Either.Right(createIntent().copy(memo = null, comment = null)))
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Handled {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN status and hash WHEN convert THEN the intent is handled unless created without a hash`(
            model: HandledModel,
        ) {
            // Act
            val actual = converter.convert(createResponse(status = model.status, txHash = model.txHash))

            // Assert
            assertThat(actual.getOrNull()?.isHandled).isEqualTo(model.isHandled)
        }

        private fun provideTestModels() = listOf(
            HandledModel(status = SendIntentStatus.CREATED, txHash = null, isHandled = false),
            HandledModel(status = SendIntentStatus.CREATED, txHash = "", isHandled = false),
            HandledModel(status = SendIntentStatus.CREATED, txHash = "0xabc", isHandled = true),
            HandledModel(status = SendIntentStatus.COMPLETED, txHash = "0xabc", isHandled = true),
            HandledModel(status = SendIntentStatus.COMPLETED, txHash = null, isHandled = true),
            HandledModel(status = SendIntentStatus.UNKNOWN, txHash = null, isHandled = true),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Amount {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN amount that is not a positive number WHEN convert THEN it is malformed`(amount: String) {
            // Act
            val actual = converter.convert(createResponse(amount = amount))

            // Assert
            assertThat(actual).isEqualTo(Either.Left(SendIntentError.MalformedAmount(INTENT_ID, amount)))
        }

        private fun provideTestModels() = listOf("", "abc", "12,5", "0", "0.00", "-1")
    }

    data class HandledModel(val status: SendIntentStatus, val txHash: String?, val isHandled: Boolean)

    private fun createIntent() = SendIntent(
        id = INTENT_ID,
        clientId = "https://agent.example.com/client.json",
        sessionId = "session",
        isHandled = false,
        networkId = "polygon-pos",
        tokenId = "tether",
        fromAddress = "0xd8da6bf26964af9d7eed9e03e53415d37aa96045",
        toAddress = "0x1111111111111111111111111111111111111111",
        amount = BigDecimal("12.5"),
        memo = "12345",
        comment = "Rent for October",
    )

    private fun createResponse(
        status: SendIntentStatus = SendIntentStatus.CREATED,
        amount: String = "12.5",
        memo: String? = "12345",
        comment: String? = "Rent for October",
        txHash: String? = null,
    ) = SendIntentResponse(
        intentId = INTENT_ID,
        status = status,
        networkId = "polygon-pos",
        fromAddress = "0xd8da6bf26964af9d7eed9e03e53415d37aa96045",
        toAddress = "0x1111111111111111111111111111111111111111",
        tokenId = "tether",
        amount = amount,
        memo = memo,
        comment = comment,
        txHash = txHash,
        clientId = "https://agent.example.com/client.json",
        sessionId = "session",
    )

    private companion object {
        const val INTENT_ID = "3f7c2a1e-9b4d-4c6a-8e21-5d0f7a9b1c33"
    }
}