package com.tangem.data.walletconnect.request

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.squareup.moshi.Moshi
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.walletconnect.network.bitcoin.WcBitcoinNetwork
import com.tangem.data.walletconnect.network.bitcoin.WcBitcoinSignMessageUseCase
import com.tangem.data.walletconnect.network.ethereum.WcEthMessageSignUseCase
import com.tangem.data.walletconnect.network.ethereum.WcEthNetwork
import com.tangem.data.walletconnect.network.solana.WcSolanaMessageSignUseCase
import com.tangem.data.walletconnect.network.solana.WcSolanaNetwork
import com.tangem.data.walletconnect.network.solana.WcSolanaSignTransactionUseCase
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.data.walletconnect.utils.WcNetworksConverter
import com.tangem.domain.models.account.Account
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk

/** Real Eth, Solana and Bitcoin request handlers over mocked dependencies. */
internal class WcRequestHandlersFixture {

    val ethMessageSignFactory: WcEthMessageSignUseCase.Factory = mockk(relaxed = true)
    val solanaMessageSignFactory: WcSolanaMessageSignUseCase.Factory = mockk(relaxed = true)
    val solanaSignTransactionFactory: WcSolanaSignTransactionUseCase.Factory = mockk(relaxed = true)
    val bitcoinSignMessageFactory: WcBitcoinSignMessageUseCase.Factory = mockk(relaxed = true)

    private val sessionsManager: WcSessionsManager = mockk(relaxed = true)
    private val networksConverter: WcNetworksConverter = mockk(relaxed = true)
    private val currencyFactory = MockCryptoCurrencyFactory()

    val ethNetwork = WcEthNetwork(
        moshi = Moshi.Builder().build(),
        sessionsManager = sessionsManager,
        factories = WcEthNetwork.Factories(
            messageSign = ethMessageSignFactory,
            signTypedData = mockk(relaxed = true),
            sendTransaction = mockk(relaxed = true),
            signTransaction = mockk(relaxed = true),
            addNetwork = mockk(relaxed = true),
            switchNetwork = mockk(relaxed = true),
        ),
        networksConverter = networksConverter,
        analyticsExceptionHandler = mockk(relaxed = true),
        chainRequestResolver = mockk(relaxed = true),
    )

    val solanaNetwork = WcSolanaNetwork(
        moshi = Moshi.Builder().build(),
        sessionsManager = sessionsManager,
        factories = WcSolanaNetwork.Factories(
            messageSign = solanaMessageSignFactory,
            signTransaction = solanaSignTransactionFactory,
            signAllTransaction = mockk(relaxed = true),
            signAndSendTransaction = mockk(relaxed = true),
        ),
        networksConverter = networksConverter,
    )

    val bitcoinNetwork = WcBitcoinNetwork(
        moshi = Moshi.Builder().build(),
        sessionsManager = sessionsManager,
        factories = WcBitcoinNetwork.Factories(
            sendTransfer = mockk(relaxed = true),
            getAccountAddresses = mockk(relaxed = true),
            signPsbt = mockk(relaxed = true),
            signMessage = bitcoinSignMessageFactory,
        ),
        networksConverter = networksConverter,
        walletManagersFacade = mockk(relaxed = true),
    )

    val handlers: Set<WcRequestToUseCaseConverter> = setOf(ethNetwork, solanaNetwork, bitcoinNetwork)

    val namespaceConverters: Set<WcNamespaceConverter> = setOf(
        WcEthNetwork.NamespaceConverter(ExcludedBlockchains()),
        WcSolanaNetwork.NamespaceConverter(ExcludedBlockchains()),
        WcBitcoinNetwork.NamespaceConverter(ExcludedBlockchains()),
    )

    fun reset() {
        clearMocks(
            ethMessageSignFactory,
            solanaMessageSignFactory,
            solanaSignTransactionFactory,
            bitcoinSignMessageFactory,
            sessionsManager,
            networksConverter,
        )
        coEvery { sessionsManager.findSessionByTopic(TOPIC) } returns createSession()
        coEvery { networksConverter.findWalletNetworkForRequest(any(), any(), any()) } answers {
            val blockchain = namespaceConverters.firstNotNullOf {
                it.toBlockchain(firstArg<WcSdkSessionRequest>().chainId.orEmpty())
            }
            currencyFactory.createCoin(blockchain).network
        }
    }

    fun createRequest(method: String, chainId: String, params: String = "") = WcSdkSessionRequest(
        topic = TOPIC,
        chainId = chainId,
        dAppMetaData = emptyMetadata(),
        request = WcSdkSessionRequest.JSONRPCRequest(id = 1L, method = method, params = params),
    )

    private fun createSession(): WcSession {
        val wallet = MockUserWalletFactory.create()
        return WcSession(
            wallet = wallet,
            networks = setOf(),
            account = Account.Personal.createMainAccount(wallet.walletId),
            securityStatus = CheckDAppResult.FAILED_TO_VERIFY,
            connectingTime = 0L,
            sdkModel = WcSdkSession(topic = TOPIC, namespaces = mapOf(), appMetaData = emptyMetadata()),
            shouldShowWalletInfo = false,
        )
    }

    private fun emptyMetadata() = WcAppMetaData(name = "", description = "", url = "", icons = listOf(), redirect = "")

    companion object {
        const val TOPIC = "topic"
        const val ETH_MAINNET = "eip155:1"
        const val SOLANA_MAINNET = "solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp"
        const val BITCOIN_MAINNET = "bip122:000000000019d6689c085ae165831e93"

        const val SOLANA_SIGN_MESSAGE_PARAMS = """{"pubkey":"pubkey","message":"2NEpo7TZRRrLZSi2U"}"""
        const val SOLANA_SIGN_TRANSACTION_PARAMS = """{"transaction":"AQID"}"""
        const val ETH_PERSONAL_SIGN_PARAMS = """["0x48656c6c6f","0xabc"]"""
        const val BITCOIN_SIGN_MESSAGE_PARAMS = """{"account":"bc1qaccount","message":"hello"}"""
    }
}