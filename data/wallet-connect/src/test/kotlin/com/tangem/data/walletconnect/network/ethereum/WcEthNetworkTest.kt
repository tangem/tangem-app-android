package com.tangem.data.walletconnect.network.ethereum

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.data.walletconnect.utils.WcNetworksConverter
import com.tangem.domain.models.account.Account
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class WcEthNetworkTest {

    private val sessionsManager: WcSessionsManager = mockk {
        every { sessions } returns emptyFlow()
        coEvery { findSessionByTopic("topic") } returns testSession()
    }
    private val networksConverter: WcNetworksConverter = mockk(relaxed = true)
    private val analyticsExceptionHandler: AnalyticsExceptionHandler = mockk(relaxed = true)
    private val signTypedDataFactory: WcEthSignTypedDataUseCase.Factory = mockk(relaxed = true)

    private val network = WcEthNetwork(
        moshi = Moshi.Builder().build(),
        sessionsManager = sessionsManager,
        factories = WcEthNetwork.Factories(
            messageSign = mockk(relaxed = true),
            signTypedData = signTypedDataFactory,
            sendTransaction = mockk(relaxed = true),
            signTransaction = mockk(relaxed = true),
            addNetwork = mockk(relaxed = true),
            switchNetwork = mockk(relaxed = true),
        ),
        networksConverter = networksConverter,
        analyticsExceptionHandler = analyticsExceptionHandler,
    )

    @Test
    fun `GIVEN non-JSON params WHEN toUseCase THEN returns parse error`() = runTest {
        // Arrange
        val request = createTypedDataRequest(params = NON_JSON_PARAMS_STRING)

        // Act
        val result = network.toUseCase(request)

        // Assert
        assertThat(result.leftOrNull())
            .isEqualTo(HandleMethodError.UnknownError("Failed to parse typed data params"))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "[]",
            "[\"0xabc\"]",
        ],
    )
    fun `GIVEN malformed params WHEN toUseCase THEN returns error`(params: String) = runTest {
        // Arrange
        val request = createTypedDataRequest(params = params)

        // Act
        val result = network.toUseCase(request)

        // Assert
        assertThat(result.leftOrNull()).isInstanceOf(HandleMethodError.UnknownError::class.java)
    }

    @Test
    fun `GIVEN malformed params WHEN toUseCase THEN reports exception with payload and dApp`() = runTest {
        // Arrange
        val metaData = WcAppMetaData(
            name = "Uniswap",
            description = "",
            url = "https://app.uniswap.org",
            icons = listOf(),
            redirect = null,
        )
        val request = createTypedDataRequest(params = NON_JSON_PARAMS_STRING, dAppMetaData = metaData)
        val eventSlot = slot<ExceptionAnalyticsEvent>()
        every { analyticsExceptionHandler.sendException(capture(eventSlot)) } returns Unit

        // Act
        network.toUseCase(request)

        // Assert
        verify(exactly = 1) { analyticsExceptionHandler.sendException(any()) }
        assertThat(eventSlot.captured.params).containsExactly(
            "method", "eth_signTypedData",
            "dApp_name", "Uniswap",
            "dApp_url", "https://app.uniswap.org",
            "params", NON_JSON_PARAMS_STRING,
        )
    }

    @Test
    fun `GIVEN well-formed params WHEN toUseCase THEN does not report exception`() = runTest {
        // Arrange
        val request = createTypedDataRequest(params = "[\"0xabc\", {}]")

        // Act
        network.toUseCase(request)

        // Assert
        verify(exactly = 0) { analyticsExceptionHandler.sendException(any()) }
    }

    @Test
    fun `GIVEN stringified typed data WHEN toUseCase THEN parses typed data`() = runTest {
        // Arrange: typedData is passed as a stringified (escaped) JSON value, not as a raw JSON object.
        val request = createTypedDataRequest(
            params = """["0xabc", "{\"message\":{\"contents\":\"hi\"}}"]""",
        )
        val methodSlot = slot<WcEthMethod.SignTypedData>()
        every { signTypedDataFactory.create(any(), capture(methodSlot)) } returns mockk(relaxed = true)

        // Act
        network.toUseCase(request)

        // Assert
        val method = methodSlot.captured
        assertThat(method.account).isEqualTo("0xabc")
        assertThat(method.params.message?.contents).isEqualTo("hi")
    }

    private fun createTypedDataRequest(
        params: String,
        dAppMetaData: WcAppMetaData = emptyMetadata(),
    ): WcSdkSessionRequest {
        return WcSdkSessionRequest(
            topic = "topic",
            chainId = "eip155:1",
            dAppMetaData = dAppMetaData,
            request = WcSdkSessionRequest.JSONRPCRequest(
                id = 1L,
                method = "eth_signTypedData",
                params = params,
            ),
        )
    }

    private fun testSession(): WcSession {
        val wallet = MockUserWalletFactory.create()
        return WcSession(
            wallet = wallet,
            networks = setOf(),
            account = Account.CryptoPortfolio.createMainAccount(wallet.walletId),
            securityStatus = CheckDAppResult.FAILED_TO_VERIFY,
            connectingTime = 0L,
            sdkModel = WcSdkSession(
                topic = "topic",
                namespaces = mapOf(),
                appMetaData = emptyMetadata(),
            ),
            showWalletInfo = false,
        )
    }

    private fun emptyMetadata(): WcAppMetaData {
        return WcAppMetaData(name = "", description = "", url = "", icons = listOf(), redirect = "")
    }

    private companion object {
        const val NON_JSON_PARAMS_STRING = "malformed_params"
    }
}