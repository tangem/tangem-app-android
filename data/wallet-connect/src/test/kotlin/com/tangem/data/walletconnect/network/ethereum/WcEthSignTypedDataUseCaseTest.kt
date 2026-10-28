package com.tangem.data.walletconnect.network.ethereum

import app.cash.turbine.test
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.domain.models.account.Account
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcEthSignTypedDataParams
import com.tangem.domain.walletconnect.model.WcRequestError
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcSignStep
import com.tangem.domain.walletmanager.WalletManagersFacade
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class WcEthSignTypedDataUseCaseTest {

    private val signUseCase: SignUseCase = mockk()
    private val respondService: WcRespondService = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val blockAidDelegate: BlockAidVerificationDelegate = mockk(relaxed = true)

    private val rawSdkRequest = WcSdkSessionRequest(
        topic = "",
        chainId = "eip155:1",
        dAppMetaData = WcAppMetaData(name = "", description = "", url = "", icons = emptyList(), redirect = ""),
        request = WcSdkSessionRequest.JSONRPCRequest(id = 0L, method = "eth_signTypedData_v4", params = ""),
    )

    private val context: WcMethodUseCaseContext = WcMethodUseCaseContext(
        network = MockCryptoCurrencyFactory().ethereum.network,
        accountAddress = SIGNER,
        rawSdkRequest = rawSdkRequest,
        networkDerivationsCount = 1,
        session = WcSession(
            wallet = MockUserWalletFactory.create(),
            networks = emptySet(),
            account = Account.Personal.createMainAccount(MockUserWalletFactory.create().walletId),
            securityStatus = CheckDAppResult.FAILED_TO_VERIFY,
            connectingTime = 0L,
            sdkModel = WcSdkSession(
                topic = "",
                namespaces = emptyMap(),
                appMetaData = WcAppMetaData(name = "", description = "", url = "", icons = emptyList(), redirect = ""),
            ),
            shouldShowWalletInfo = false,
        ),
    )

    @BeforeEach
    fun setup() {
        clearMocks(signUseCase, respondService, analytics, walletManagersFacade, blockAidDelegate)
        every { respondService.isRequestActual(any()) } returns true
    }

    @Test
    fun `GIVEN typed data with account as verifyingContract WHEN invoke THEN emits forbidden error without signing`() =
        runTest {
            // Arrange
            val useCase = createUseCase(dataForSign = FORBIDDEN_TYPED_DATA)

            // Act
            val states = useCase.invoke().toList()

            // Assert
            assertThat(states).hasSize(1)
            val result = (states.single().domainStep as WcSignStep.Result).result
            assertThat(result.leftOrNull()).isEqualTo(WcRequestError.ForbiddenRequest)
            coVerify(exactly = 0) { signUseCase(hash = any(), userWallet = any(), network = any()) }
            coVerify(exactly = 0) { respondService.respond(any(), any()) }
            verify(exactly = 0) { respondService.rejectRequestNonBlock(any(), any()) }
        }

    @Test
    fun `GIVEN dApp account differs from session account WHEN invoke THEN checks session account`() = runTest {
        // Arrange
        val useCase = createUseCase(dataForSign = FORBIDDEN_TYPED_DATA, account = OTHER_ACCOUNT)

        // Act
        val state = useCase.invoke().first()

        // Assert
        val result = (state.domainStep as WcSignStep.Result).result
        assertThat(result.leftOrNull()).isEqualTo(WcRequestError.ForbiddenRequest)
    }

    @Test
    fun `GIVEN forbidden typed data WHEN collect securityStatus THEN BlockAid is not requested`() = runTest {
        // Arrange
        val useCase = createUseCase(dataForSign = FORBIDDEN_TYPED_DATA)

        // Act
        val securityStates = useCase.securityStatus.toList()

        // Assert
        assertThat(securityStates).isEmpty()
        verify(exactly = 0) { blockAidDelegate.getSecurityStatus(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN forbidden typed data WHEN cancel after error result THEN rejects once without cancel analytics`() =
        runTest {
            // Arrange
            val useCase = createUseCase(dataForSign = FORBIDDEN_TYPED_DATA)
            useCase.invoke().toList()

            // Act
            useCase.cancel()

            // Assert
            verify(exactly = 1) { respondService.rejectRequestNonBlock(rawSdkRequest, any()) }
            verify(exactly = 0) { analytics.send(any()) }
        }

    @Test
    fun `GIVEN regular typed data WHEN invoke THEN emits pre-sign state`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val useCase = createUseCase(dataForSign = ALLOWED_TYPED_DATA)

        // Act
        useCase.invoke().test {
            val state = awaitItem()

            // Assert
            assertThat(state.domainStep).isEqualTo(WcSignStep.PreSign)
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 1) { blockAidDelegate.getSecurityStatus(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { respondService.rejectRequestNonBlock(any(), any()) }
    }

    private fun createUseCase(dataForSign: String, account: String = SIGNER) = WcEthSignTypedDataUseCase(
        respondService = respondService,
        analytics = analytics,
        context = context,
        method = WcEthMethod.SignTypedData(
            params = WcEthSignTypedDataParams(message = null),
            account = account,
            dataForSign = dataForSign,
        ),
        walletManagersFacade = walletManagersFacade,
        signUseCase = signUseCase,
        blockAidDelegate = blockAidDelegate,
    )

    private companion object {
        const val SIGNER = "0x1234567890abcdef1234567890abcdef12345678"
        const val OTHER_ACCOUNT = "0x00000000000000000000000000000000000000aa"
        const val FORBIDDEN_TYPED_DATA = """{"types":{},"primaryType":"Mail",""" +
            """"domain":{"name":"Mail","verifyingContract":"0x1234567890ABCDEF1234567890ABCDEF12345678"},""" +
            """"message":{}}"""
        const val ALLOWED_TYPED_DATA = """{"types":{},"primaryType":"Permit",""" +
            """"domain":{"name":"USD Coin","verifyingContract":"0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"},""" +
            """"message":{}}"""
    }
}