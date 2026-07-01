package com.tangem.data.walletconnect.network.solana

import app.cash.turbine.test
import arrow.core.right
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.models.account.Account
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.domain.walletconnect.model.WcRequestError
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcSignStep
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class WcSolanaMessageSignUseCaseTest {

    private val signUseCase: SignUseCase = mockk()
    private val respondService: WcRespondService = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    private val context: WcMethodUseCaseContext = WcMethodUseCaseContext(
        network = MockCryptoCurrencyFactory().ethereum.network,
        accountAddress = "",
        rawSdkRequest = WcSdkSessionRequest(
            topic = "",
            chainId = "",
            dAppMetaData = WcAppMetaData(name = "", description = "", url = "", icons = listOf(), redirect = ""),
            request = WcSdkSessionRequest.JSONRPCRequest(id = 0L, method = "", params = ""),
        ),
        networkDerivationsCount = 1,
        session = WcSession(
            wallet = MockUserWalletFactory.create(),
            networks = setOf(),
            account = Account.CryptoPortfolio.createMainAccount(MockUserWalletFactory.create().walletId),
            securityStatus = CheckDAppResult.FAILED_TO_VERIFY,
            connectingTime = 0L,
            sdkModel = WcSdkSession(
                topic = "",
                namespaces = mapOf(),
                appMetaData = WcAppMetaData(name = "", description = "", url = "", icons = listOf(), redirect = ""),
            ),
            showWalletInfo = false,
        ),
    )

    @BeforeEach
    fun setup() {
        clearMocks(signUseCase, respondService)
    }

    @Test
    fun `GIVEN payload is a serialized transaction WHEN sign THEN request rejected without signing`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val useCase = createUseCase(rawMessage = LEGACY_TRANSACTION_MESSAGE.encodeBase58())

            // Act
            useCase.invoke().test {
                awaitItem() // initial PreSign state
                useCase.sign()
                val result = (expectMostRecentItem().domainStep as WcSignStep.Result).result

                // Assert
                assertTrue(result.isLeft())
                assertTrue(result.leftOrNull() is WcRequestError.UnknownError)
            }
            coVerify(exactly = 0) {
                signUseCase(
                    hash = any(),
                    userWallet = any(),
                    network = any(),
                )
            }
            coVerify(exactly = 0) { respondService.respond(any(), any()) }
        }

    @Test
    fun `GIVEN human-readable message WHEN sign THEN it is signed and responded`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val message = "Sign in to Tangem\nNonce: 8f3a91c0d4".toByteArray()
            coEvery {
                signUseCase(
                    hash = any(),
                    userWallet = any(),
                    network = any(),
                )
            } returns byteArrayOf(0x0A, 0x0B, 0x0C).right()
            coEvery { respondService.respond(any(), any()) } returns RESPOND_RESULT.right()
            val useCase = createUseCase(rawMessage = message.encodeBase58())

            // Act
            useCase.invoke().test {
                awaitItem() // initial PreSign state
                useCase.sign()
                val result = (expectMostRecentItem().domainStep as WcSignStep.Result).result

                // Assert
                assertEquals(RESPOND_RESULT.right(), result)
            }
            coVerify(exactly = 1) { signUseCase(any(), context.session.wallet, context.network) }
            coVerify(exactly = 1) { respondService.respond(any(), any()) }
        }

    private fun createUseCase(rawMessage: String) = WcSolanaMessageSignUseCase(
        context = context,
        method = WcSolanaMethod.SignMessage(pubKey = "", rawMessage = rawMessage, humanMsg = ""),
        signUseCase = signUseCase,
        respondService = respondService,
        analytics = analytics,
    )

    private companion object {
        const val RESPOND_RESULT = "{ signature: \"signature\" }"

        // A minimal but well-formed legacy Solana message: header + 2 accounts + blockhash + 1 instruction.
        val LEGACY_TRANSACTION_MESSAGE: ByteArray = byteArrayOf(0x01, 0x00, 0x01) + // message header
            byteArrayOf(0x02) + ByteArray(size = 2 * 32) + // 2 account keys
            ByteArray(size = 32) + // recent blockhash
            byteArrayOf(0x01) + // instruction count
            byteArrayOf(0x01) + // program id index
            byteArrayOf(0x01, 0x00) + // 1 account index = [0]
            byteArrayOf(0x03, 0x0A, 0x0B, 0x0C) // data length 3 + 3 data bytes
    }
}