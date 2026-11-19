package com.tangem.store.datasource.jointaccount

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointWalletNotLinkedResolverTest {

    private val resolver = JointWalletNotLinkedResolver(moshi = Moshi.Builder().build())

    @ParameterizedTest
    @ProvideTestModels
    fun isWalletNotLinked(model: ResolveModel) {
        // Act
        val actual = resolver.isWalletNotLinked(model.error)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ResolveModel(error = httpException(Code.FORBIDDEN, NOT_LINKED_BODY), expected = true),
        ResolveModel(error = httpException(Code.FORBIDDEN, """{"errorCode":"OTHER"}"""), expected = false),
        ResolveModel(error = httpException(Code.FORBIDDEN, """{"code":"WALLET_NOT_AUTHORIZED"}"""), expected = false),
        ResolveModel(error = httpException(Code.FORBIDDEN, errorBody = null), expected = false),
        ResolveModel(error = httpException(Code.FORBIDDEN, "<html>Cloudflare</html>"), expected = false),
        ResolveModel(error = httpException(Code.UNAUTHORIZED, NOT_LINKED_BODY), expected = false),
        ResolveModel(error = ApiResponseError.NetworkException(), expected = false),
    )

    internal data class ResolveModel(val error: Throwable, val expected: Boolean)

    private fun httpException(code: Code, errorBody: String?) =
        ApiResponseError.HttpException(code = code, message = null, errorBody = errorBody)

    private companion object {
        const val NOT_LINKED_BODY = """{"errorCode":"WALLET_NOT_AUTHORIZED","walletId":"wallet"}"""
    }
}