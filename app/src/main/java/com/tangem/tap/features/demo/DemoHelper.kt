package com.tangem.tap.features.demo

import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.extensions.dispatchNotification
import com.tangem.tap.common.redux.AppState
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
        val demoIds = getReleaseIds().toMutableList()
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

    private fun getReleaseIds(): List<String> {
        return (releaseDemoCardIds +
            releaseDemoCardIds_19042022).distinct()
    }

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

    //    https://tangem.slack.com/archives/GMXC6PP71/p1650360610759269
    private val releaseDemoCardIds_19042022 = listOf(
        //        Wallet
        "AC01000000045754",
        "AC01000000041662",
        "AC01000000041647",
        "AC01000000041209",
        "AC01000000042462",
        "AC01000000041100",
        "AC01000000041621",
        "AC01000000045960",
        "AC01000000041092",
        "AC01000000041217",
        "AC01000000013489",
        "AC01000000028610",
        "AC01000000028701",
        "AC01000000028578",
        "AC01000000027281",
        "AC01000000027216",
        "AC01000000028594",
        "AC01000000028602",
        "AC01000000028636",
        "AC01000000013968",
        "AC01000000027208",
        "AC01000000013471",
        "AC01000000028586",
        "AC01000000013703",
        "AC01000000028628",
        "AC01000000028693",
        "AC01000000028685",
        "AC01000000013950",
        "AC01000000013828",
        "AC01000000013497",
        "AC01000000013836",
        "AC01000000013505",
//            Note BTC
        "AB01000000059608",
        "AB01000000046647",
        "AB01000000046571",
        "AB01000000046746",
        "AB01000000059574",
        "AB01000000046753",
        "AB01000000046605",
        "AB01000000046761",
        "AB01000000046720",
        "AB01000000046530",
        "AB01000000016475",
        "AB01000000016483",
        "AB01000000016491",
        "AB01000000020709",
        "AB01000000020717",
        "AB01000000015550",
        "AB01000000015394",
        "AB01000000016079",
        "AB01000000016087",
        "AB01000000016095",
        "AB01000000020915",
        "AB01000000017184",
        "AB01000000020907",
        "AB01000000017192",
        "AB01000000016210",
        "AB01000000016111",
        "AB01000000016103",
        "AB01000000015766",
        "AB01000000015774",
        "AB01000000015782",
        "AB01000000022598",
        "AB01000000022580",
//            Note ETH
        "AB02000000051083",
        "AB02000000051059",
        "AB02000000051158",
        "AB02000000050986",
        "AB02000000051026",
        "AB02000000050960",
        "AB02000000051042",
        "AB02000000051091",
        "AB02000000051034",
        "AB02000000051133",
        "AB02000000019924",
        "AB02000000019932",
        "AB02000000022092",
        "AB02000000022282",
        "AB02000000023983",
        "AB02000000023439",
        "AB02000000020328",
        "AB02000000020310",
        "AB02000000021565",
        "AB02000000022357",
        "AB02000000023355",
        "AB02000000022324",
        "AB02000000022100",
        "AB02000000019999",
        "AB02000000020013",
        "AB02000000020005",
        "AB02000000020021",
        "AB02000000020039",
        "AB02000000020278",
        "AB02000000020252",
        "AB02000000018652",
        "AB02000000018561",
    )

    private val testDemoCardIds = listOf(
        "FB20000000000186", // Note ETH
        "FB10000000000196", // Note BTC
        "FB30000000000176", // Wallet
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