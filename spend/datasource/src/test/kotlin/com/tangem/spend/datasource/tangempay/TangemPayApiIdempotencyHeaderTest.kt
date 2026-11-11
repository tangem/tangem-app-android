package com.tangem.spend.datasource.tangempay

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
        model: HeaderModel,
    ) {
        // Act
        val headers = model.api.methods
            .single { it.name == model.methodName }
            .parameterAnnotations
            .flatMap { annotations -> annotations.filterIsInstance<Header>() }
            .map(Header::value)

        // Assert
        assertThat(headers).contains("X-Idempotency-Key")
    }

    private fun provideTestModels() = listOf(
        HeaderModel(api = TangemPayApi::class.java, methodName = "createOrder"),
        HeaderModel(api = TangemPayApi::class.java, methodName = "createBankOfframpOrder"),
        HeaderModel(api = TangemPayApi::class.java, methodName = "submitBankOfframpSignature"),
        HeaderModel(api = TangemPayApi::class.java, methodName = "createBankOfframpRoute"),
        HeaderModel(api = TangemPayApi::class.java, methodName = "createBankOfframpQuote"),
        HeaderModel(api = TangemPayApi::class.java, methodName = "joinWaitlist"),
        HeaderModel(api = TangemPayOrderApi::class.java, methodName = "createOrder"),
    )

    internal data class HeaderModel(val api: Class<*>, val methodName: String) {
        override fun toString(): String = "${api.simpleName}.$methodName"
    }
}