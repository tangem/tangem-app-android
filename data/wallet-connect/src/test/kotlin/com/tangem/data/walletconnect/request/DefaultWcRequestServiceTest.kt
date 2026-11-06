package com.tangem.data.walletconnect.request

import com.reown.walletkit.client.Wallet
import com.tangem.data.walletconnect.request.WcRequestHandlersFixture.Companion.BITCOIN_MAINNET
import com.tangem.data.walletconnect.request.WcRequestHandlersFixture.Companion.ETH_MAINNET
import com.tangem.data.walletconnect.request.WcRequestHandlersFixture.Companion.SOLANA_MAINNET
import com.tangem.data.walletconnect.request.WcRequestHandlersFixture.Companion.TOPIC
import com.tangem.data.walletconnect.respond.DefaultWcRespondService
import com.tangem.domain.walletconnect.model.WcJsonRpcError
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultWcRequestServiceTest {

    private val fixture = WcRequestHandlersFixture()
    private val respondService: DefaultWcRespondService = mockk(relaxed = true)
    private val verifyContext: Wallet.Model.VerifyContext = mockk(relaxed = true)

    private val service = DefaultWcRequestService(
        requestConverters = fixture.handlers,
        respondService = respondService,
    )

    @BeforeEach
    fun setup() {
        clearMocks(respondService)
        every { verifyContext.origin } returns ""
        every { verifyContext.verifyUrl } returns ""
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN method and chainId WHEN onSessionRequest THEN rejected as unsupported only for foreign namespace`(
        model: DispatchModel,
    ) {
        // Arrange
        val sessionRequest = createSessionRequest(method = model.method, chainId = model.chainId)

        // Act
        service.onSessionRequest(sessionRequest, verifyContext)

        // Assert
        val expectedRejections = if (model.isRejected) 1 else 0
        verify(exactly = expectedRejections) {
            respondService.rejectRequestNonBlock(any(), WcJsonRpcError.UnsupportedMethod)
        }
    }

    private fun createSessionRequest(method: String, chainId: String): Wallet.Model.SessionRequest {
        val jsonRpcRequest: Wallet.Model.SessionRequest.JSONRPCRequest = mockk {
            every { id } returns 1L
            every { this@mockk.method } returns method
            every { params } returns ""
        }
        return mockk {
            every { topic } returns TOPIC
            every { this@mockk.chainId } returns chainId
            every { peerMetaData } returns null
            every { request } returns jsonRpcRequest
        }
    }

    data class DispatchModel(val method: String, val chainId: String, val isRejected: Boolean)

    private fun provideTestModels() = listOf(
        DispatchModel("solana_signMessage", ETH_MAINNET, isRejected = true),
        DispatchModel("solana_signTransaction", ETH_MAINNET, isRejected = true),
        DispatchModel("personal_sign", SOLANA_MAINNET, isRejected = true),
        DispatchModel("eth_sendTransaction", SOLANA_MAINNET, isRejected = true),
        DispatchModel("signPsbt", ETH_MAINNET, isRejected = true),
        DispatchModel("solana_signMessage", SOLANA_MAINNET, isRejected = false),
        DispatchModel("solana_signTransaction", SOLANA_MAINNET, isRejected = false),
        DispatchModel("personal_sign", ETH_MAINNET, isRejected = false),
        DispatchModel("eth_sendTransaction", ETH_MAINNET, isRejected = false),
        DispatchModel("signPsbt", BITCOIN_MAINNET, isRejected = false),
    )
}