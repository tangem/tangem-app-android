package com.tangem.data.walletconnect.network.ethereum

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sdk.WcSdk
import com.tangem.domain.walletconnect.model.WcEthMethodName
import com.tangem.domain.walletconnect.model.WcJsonRpcError
import com.tangem.domain.walletconnect.model.WcRequestError
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class WcEthChainRequestResolverTest {

    private val wcSdk: WcSdk = mockk()
    private val respondService: WcRespondService = mockk()
    private val chainChangedEmitter: WcEthChainChangedEmitter = mockk(relaxed = true)

    private val resolver = WcEthChainRequestResolver(
        moshi = Moshi.Builder().build(),
        wcSdk = wcSdk,
        respondService = respondService,
        chainChangedEmitter = chainChangedEmitter,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(wcSdk, respondService, chainChangedEmitter)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Resolve {

        @ParameterizedTest
        @ProvideTestModels
        fun resolve(model: ResolveModel) = runTest {
            // Arrange
            every { wcSdk.sessionCarriesChain(topic = TOPIC, rawChainId = any()) } returns model.isInSession

            // Act
            val actual = resolver.resolve(methodName = model.methodName, request = request(params = model.params))

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        @Test
        fun `GIVEN the session cannot be read WHEN resolve THEN leaves the request to the existing flow`() =
            runTest {
                // Arrange
                every { wcSdk.sessionCarriesChain(topic = TOPIC, rawChainId = any()) } returns null

                // Act
                val actual = resolver.resolve(
                    methodName = WcEthMethodName.SwitchEthereumChain,
                    request = request(params = ETHEREUM_PARAMS),
                )

                // Assert
                assertThat(actual).isNull()
            }

        @Test
        fun `GIVEN another method WHEN resolve THEN leaves the request to the existing flow`() = runTest {
            // Act
            val actual = resolver.resolve(
                methodName = WcEthMethodName.SendTransaction,
                request = request(params = ETHEREUM_PARAMS),
            )

            // Assert
            assertThat(actual).isNull()
        }

        private fun provideTestModels() = listOf(
            ResolveModel(
                methodName = WcEthMethodName.SwitchEthereumChain,
                isInSession = true,
                expected = WcEthChainRequestResolver.Resolution.Switched(ETHEREUM_CAIP2),
            ),
            ResolveModel(
                methodName = WcEthMethodName.SwitchEthereumChain,
                isInSession = false,
                expected = WcEthChainRequestResolver.Resolution.UnrecognizedChain,
            ),
            ResolveModel(
                methodName = WcEthMethodName.AddEthereumChain,
                isInSession = true,
                expected = WcEthChainRequestResolver.Resolution.AlreadyAdded(ETHEREUM_CAIP2),
            ),
            ResolveModel(
                methodName = WcEthMethodName.AddEthereumChain,
                isInSession = false,
                expected = WcEthChainRequestResolver.Resolution.NeedsUserApproval,
            ),
            ResolveModel(
                methodName = WcEthMethodName.SwitchEthereumChain,
                params = """[{"chainId": "0x1"}, {"chainId": "0x89"}]""",
                expected = WcEthChainRequestResolver.Resolution.InvalidParams,
            ),
            ResolveModel(
                methodName = WcEthMethodName.SwitchEthereumChain,
                params = "[]",
                expected = WcEthChainRequestResolver.Resolution.InvalidParams,
            ),
            ResolveModel(
                methodName = WcEthMethodName.SwitchEthereumChain,
                params = """[{"chainId": "not-a-number"}]""",
                expected = WcEthChainRequestResolver.Resolution.InvalidParams,
            ),
            ResolveModel(
                methodName = WcEthMethodName.AddEthereumChain,
                params = "malformed",
                expected = WcEthChainRequestResolver.Resolution.InvalidParams,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Respond {

        @ParameterizedTest
        @ProvideTestModels
        fun respond(model: RespondModel) = runTest {
            // Arrange
            coEvery { respondService.respond(any(), any()) } returns "".right()
            every { respondService.rejectRequestNonBlock(any(), any()) } returns Unit

            // Act
            resolver.respond(request = request(params = ETHEREUM_PARAMS), resolution = model.resolution)

            // Assert
            val successCount = if (model.isAnsweredWithSuccess) 1 else 0
            coVerify(exactly = successCount) { respondService.respond(any(), null) }
            coVerify(exactly = successCount) { chainChangedEmitter.emit(topic = TOPIC, chainId = ETHEREUM_CAIP2) }
            model.expectedError?.let { error ->
                verify(exactly = 1) { respondService.rejectRequestNonBlock(any(), error) }
            }
            verify(exactly = if (model.expectedError == null) 0 else 1) {
                respondService.rejectRequestNonBlock(any(), any())
            }
        }

        @Test
        fun `GIVEN failed respond WHEN respond THEN chainChanged is not emitted`() = runTest {
            // Arrange
            coEvery { respondService.respond(any(), any()) } returns WcRequestError.UnknownError().left()

            // Act
            resolver.respond(
                request = request(params = ETHEREUM_PARAMS),
                resolution = WcEthChainRequestResolver.Resolution.Switched(ETHEREUM_CAIP2),
            )

            // Assert
            coVerify(exactly = 0) { chainChangedEmitter.emit(any(), any()) }
        }

        private fun provideTestModels() = listOf(
            RespondModel(
                resolution = WcEthChainRequestResolver.Resolution.Switched(ETHEREUM_CAIP2),
                isAnsweredWithSuccess = true,
            ),
            RespondModel(
                resolution = WcEthChainRequestResolver.Resolution.AlreadyAdded(ETHEREUM_CAIP2),
                isAnsweredWithSuccess = true,
            ),
            RespondModel(
                resolution = WcEthChainRequestResolver.Resolution.UnrecognizedChain,
                expectedError = WcJsonRpcError.UnrecognizedChain,
            ),
            RespondModel(
                resolution = WcEthChainRequestResolver.Resolution.InvalidParams,
                expectedError = WcJsonRpcError.InvalidParams,
            ),
            RespondModel(resolution = WcEthChainRequestResolver.Resolution.NeedsUserApproval),
        )
    }

    internal data class ResolveModel(
        val methodName: WcEthMethodName,
        val expected: WcEthChainRequestResolver.Resolution,
        val params: String = ETHEREUM_PARAMS,
        val isInSession: Boolean = false,
    )

    internal data class RespondModel(
        val resolution: WcEthChainRequestResolver.Resolution,
        val isAnsweredWithSuccess: Boolean = false,
        val expectedError: WcJsonRpcError? = null,
    )

    private fun request(params: String) = WcSdkSessionRequest(
        topic = TOPIC,
        chainId = "eip155:137",
        dAppMetaData = WcAppMetaData(name = "", description = "", url = "", icons = listOf(), redirect = ""),
        request = WcSdkSessionRequest.JSONRPCRequest(id = 1L, method = "wallet_switchEthereumChain", params = params),
    )



    private companion object {
        const val TOPIC = "topic"
        const val ETHEREUM_PARAMS = """[{"chainId": "0x1"}]"""
        val ETHEREUM_CAIP2 = CAIP2(namespace = "eip155", reference = "1")
    }
}