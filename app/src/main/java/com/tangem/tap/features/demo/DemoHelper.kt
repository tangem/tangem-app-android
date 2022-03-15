package com.tangem.tap.features.demo

import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.tap.common.extensions.dispatchNotification
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import org.rekotlin.Action
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
interface DemoMiddleware {
    fun tryHandle(config: DemoConfig, scanResponse: ScanResponse, action: Action): Boolean
}

object DemoHelper {
    val config = DemoConfig()

    private val demoMiddlewares = listOf(
        DemoOnboardingNoteMiddleware(),
    )

    private val disabledActionFeatures = listOf(
        WalletConnectAction.StartWalletConnect::class.java,
        WalletAction.TradeCryptoAction.Buy::class.java,
        WalletAction.TradeCryptoAction.Sell::class.java,
        BackupAction.StartBackup::class.java,
        WalletAction.ExploreAddress::class.java
    )

    fun isDemoCard(scanResponse: ScanResponse): Boolean = isDemoCardId(scanResponse.card.cardId)

    fun isDemoCardId(cardId: String): Boolean = config.isDemoCardId(cardId)

    fun tryHandle(appState: () -> AppState?, action: Action): Boolean {
        val scanResponse = getScanResponse(appState) ?: return false
        if (!scanResponse.isDemoCard()) return false

        demoMiddlewares.forEach {
            if (it.tryHandle(config, scanResponse, action)) return true
        }

        disabledActionFeatures.firstOrNull { it == action::class.java }?.let {
            store.dispatchNotification(R.string.alert_demo_feature_disabled)
            return true
        }

        return false
    }

    fun injectDemoBalance(walletManager: WalletManager?) {
        val manager = walletManager ?: return

        val blockchain = walletManager.wallet.blockchain
        val amount = config.getBalance(blockchain)
        manager.wallet.setAmount(amount)
    }

    private fun getScanResponse(appState: () -> AppState?): ScanResponse? {
        val state = appState() ?: return null

        return state.globalState.onboardingState.onboardingManager?.scanResponse
            ?: state.globalState.scanResponse
    }
}

class DemoConfig {

    val demoBlockchains = listOf(
        Blockchain.Bitcoin,
        Blockchain.Ethereum,
        Blockchain.Dogecoin,
        Blockchain.Solana,
    )

    val demoCardIds: List<String> by lazy {
        val demoIds = (releaseDemoCardIds + testDemoCardIds).toMutableList()
        if (BuildConfig.DEBUG) demoIds.addAll(debugTestDemoCardIds)

        return@lazy demoIds.distinct()
    }

    private val walletBalances: Map<Blockchain, Amount> = mapOf(
        Blockchain.Bitcoin to Amount(0.028.toBigDecimal(), Blockchain.Bitcoin),
        Blockchain.Ethereum to Amount(0.2311.toBigDecimal(), Blockchain.Ethereum),
        Blockchain.Dogecoin to Amount(1450.025.toBigDecimal(), Blockchain.Dogecoin),
        Blockchain.Solana to Amount(13.246.toBigDecimal(), Blockchain.Solana),
    )

    fun isDemoCardId(cardId: String): Boolean = demoCardIds.contains(cardId)

    fun getBalance(blockchain: Blockchain): Amount = walletBalances[blockchain]?.copy()
        ?: Amount(BigDecimal.ZERO, blockchain).copy()

    private val releaseDemoCardIds = mutableListOf<String>(
//        Tangem Wallet:
        "AC01000000041100",
        "AC01000000042462",
        "AC01000000041647",
        "AC01000000041621",
        "AC01000000041217",
        "AC01000000041225",
        "AC01000000041209",
        "AC01000000041092",
        "AC01000000041472",
        "AC01000000041662",
        "AC01000000045754",
        "AC01000000045960",
//            Tangem Note BTC:
        "AB01000000046530",
        "AB01000000046720",
        "AB01000000046746",
        "AB01000000046498",
        "AB01000000046753",
        "AB01000000049608",
        "AB01000000046761",
        "AB01000000049574",
        "AB01000000046605",
        "AB01000000046571",
        "AB01000000046704",
        "AB01000000046647",
//            Tangem Note Ethereum:
        "AB02000000051000",
        "AB02000000050986",
        "AB02000000051026",
        "AB02000000051042",
        "AB02000000051091",
        "AB02000000051083",
        "AB02000000050960",
        "AB02000000051034",
        "AB02000000050911",
        "AB02000000051133",
        "AB02000000051158",
        "AB02000000051059",
    )

    private val testDemoCardIds = listOf(
        "FB20000000000186", // Note ETH
        "FB10000000000196", // Note BTC
        "FB30000000000176", // Wallet
        //TODO: delete bellow ids before 3.28 release
        "AB01000000045060", // Note BTC
        "AB02000000045028", // Note ETH
        "AC79000000000004", // Wallet 4.46
    )

    private val debugTestDemoCardIds = listOf<String>(
    )
}

class DemoTransactionSender(
    private val walletManager: WalletManager,
    private val sender: TransactionSender = walletManager as TransactionSender
) : TransactionSender {

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        val blockchain = walletManager.wallet.blockchain
        return when (walletManager) {
            is BitcoinWalletManager -> Result.Success(listOf(
                Amount(0.0001.toBigDecimal(), blockchain),
                Amount(0.0003.toBigDecimal(), blockchain),
                Amount(0.00055.toBigDecimal(), blockchain),
            ))
            else -> sender.getFee(amount, destination)
        }
    }


    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val dataToSign = randomString(32).toByteArray()
        val signerResponse = signer.sign(dataToSign, walletManager.wallet.cardId, walletManager.wallet.publicKey)
        return when (signerResponse) {
            is CompletionResult.Success -> SimpleResult.Failure(Exception(ID))
            is CompletionResult.Failure -> SimpleResult.fromTangemSdkError(signerResponse.error)
        }
    }

    private fun randomInt(from: Int, to: Int): Int = kotlin.random.Random.nextInt(from, to)

    private fun randomString(length: Int): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { randomInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    companion object {
        val ID = DemoTransactionSender::class.java.simpleName
    }

}