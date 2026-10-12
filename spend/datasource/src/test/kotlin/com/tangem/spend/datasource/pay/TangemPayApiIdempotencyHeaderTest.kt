package com.tangem.spend.datasource.pay

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import retrofit2.http.Header

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayApiIdempotencyHeaderTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a keyed Pay request WHEN its headers are read THEN the key goes in X-Idempotency-Key`(
        methodName: String,
    ) {
        // Act
        val headers = TangemPayApi::class.java.methods
            .single { it.name == methodName }
            .parameterAnnotations
            .flatMap { annotations -> annotations.filterIsInstance<Header>() }
            .map(Header::value)

        // Assert
        assertThat(headers).contains("X-Idempotency-Key")
    }

    private fun provideTestModels() = listOf(
        "createOrder",
        "createBankOfframpOrder",
        "submitBankOfframpSignature",
        "joinWaitlist",
    )
}