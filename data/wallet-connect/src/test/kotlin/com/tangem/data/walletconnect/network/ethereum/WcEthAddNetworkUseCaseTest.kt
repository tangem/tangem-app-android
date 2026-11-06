package com.tangem.data.walletconnect.network.ethereum

import arrow.core.left
import arrow.core.right
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.reown.walletkit.client.Wallet
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sdk.WcSdk
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.WcNetworksConverter
import com.tangem.domain.models.account.Account
import com.tangem.domain.walletconnect.model.WcEthAddChain
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class WcEthAddNetworkUseCaseTest {

    private val respondService: WcRespondService = mockk()
    private val wcSdk: WcSdk = mockk()
    private val networksConverter: WcNetworksConverter = mockk()
    private val chainChangedEmitter: WcEthChainChangedEmitter = mockk(relaxed = true)
    private val delegateFactory: WcEthAddSwitchCommonDelegate.Factory = mockk()

    private val wallet = MockUserWalletFactory.create()

    private val rawSdkRequest = WcSdkSessionRequest(
        topic = TOPIC,
        chainId = "eip155:1",
        dAppMetaData = WcAppMetaData(name = "", description = "", url = "", icons = emptyList(), redirect = ""),
        request = WcSdkSessionRequest.JSONRPCRequest(
            id = 0L,
            method = "wallet_addEthereumChain",
            params = """[{"chainId": "$CHAIN_ID_HEX"}]""",
        ),
    )

    private val context = WcMethodUseCaseContext(
        network = MockCryptoCurrencyFactory().ethereum.network,
        accountAddress = ADDRESS,
        rawSdkRequest = rawSdkRequest,
        networkDerivationsCount = 1,
        session = WcSession(
            wallet = wallet,
            networks = emptySet(),
            account = Account.Personal.createMainAccount(wallet.walletId),
            securityStatus = CheckDAppResult.FAILED_TO_VERIFY,
            connectingTime = 0L,
            sdkModel = WcSdkSession(
                topic = TOPIC,
                namespaces = mapOf(
                    "eip155" to WcSdkSession.Session(
                        chains = listOf("eip155:1"),
                        accounts = listOf("eip155:1:$ADDRESS"),
                        methods = listOf("eth_sendTransaction"),
                        events = listOf("chainChanged"),
                    ),
                ),
                appMetaData = WcAppMetaData(name = "", description = "", url = "", icons = emptyList(), redirect = ""),
            ),
            shouldShowWalletInfo = false,
        ),
    )

    private val useCase by lazy {
        WcEthAddNetworkUseCase(
            respondService = respondService,
            wcSdk = wcSdk,
            networksConverter = networksConverter,
            chainChangedEmitter = chainChangedEmitter,
            addSwitchCommonDelegateFactory = delegateFactory,
            context = context,
            method = WcEthMethod.AddEthereumChain(rawChain = WcEthAddChain(chainId = CHAIN_ID_HEX)),
        )
    }

    @BeforeEach
    fun setup() {
        clearMocks(respondService, wcSdk, networksConverter, chainChangedEmitter, delegateFactory)
        every { delegateFactory.create(context) } returns WcEthAddSwitchCommonDelegate(networksConverter, context)
        coEvery { networksConverter.allAddressForChain(any(), any(), any()) } returns listOf(ADDRESS)
        coEvery { respondService.respond(any(), any()) } returns "".right()
    }

    @Test
    fun `GIVEN session updated WHEN approve THEN chainChanged is emitted before the dApp is answered`() = runTest {
        // Arrange
        coEvery { wcSdk.updateSession(any()) } returns mockk<Wallet.Params.SessionUpdate>().right()

        // Act
        useCase.approve()

        // Assert
        coVerifyOrder {
            chainChangedEmitter.emit(topic = TOPIC, chainId = ADDED_CHAIN)
            respondService.respond(rawSdkRequest, null)
        }
    }

    @Test
    fun `GIVEN session update failed WHEN approve THEN the dApp is answered without chainChanged`() = runTest {
        // Arrange
        coEvery { wcSdk.updateSession(any()) } returns IllegalStateException().left()

        // Act
        useCase.approve()

        // Assert
        coVerify(exactly = 0) { chainChangedEmitter.emit(any(), any()) }
        coVerify(exactly = 1) { respondService.respond(rawSdkRequest, null) }
    }

    private companion object {
        const val TOPIC = "topic"
        const val ADDRESS = "0x891D662701e763c6bf73B57265304f51d855e2b9"
        const val CHAIN_ID_HEX = "0x1237"
        val ADDED_CHAIN = CAIP2(namespace = "eip155", reference = "4663")
    }
}