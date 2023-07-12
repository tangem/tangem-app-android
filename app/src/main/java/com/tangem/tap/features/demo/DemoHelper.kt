package com.tangem.tap.features.demo

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.common.demo.DemoConfig
import com.tangem.tap.common.extensions.dispatchNotification
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.wallet.R
import org.rekotlin.Action

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
        WalletAction.ExploreAddress::class.java,
        DetailsAction.ResetToFactory.Start::class.java,
    )

    fun isDemoCard(scanResponse: ScanResponse): Boolean = isDemoCardId(scanResponse.card.cardId)

    fun isTestDemoCard(scanResponse: ScanResponse): Boolean = config.isTestDemoCardId(scanResponse.card.cardId)

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

class DemoTransactionSender(private val walletManager: WalletManager) : TransactionSender {

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val blockchain = walletManager.wallet.blockchain
        return Result.Success(
            TransactionFee.Choosable(
                minimum = Fee.Common(Amount(minimumFee, blockchain)),
                normal = Fee.Common(Amount(normalFee, blockchain)),
                priority = Fee.Common(Amount(priorityFee, blockchain)),
            ),
        )
    }

    @Suppress("MagicNumber")
    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val dataToSign = randomString(32).toByteArray()
        val signerResponse = signer.sign(
            hash = dataToSign,
            publicKey = walletManager.wallet.publicKey,
        )
        return when (signerResponse) {
            is CompletionResult.Success -> SimpleResult.Failure(Exception(ID).toBlockchainSdkError())
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

        private val minimumFee = 0.0001.toBigDecimal()
        private val normalFee = 0.0002.toBigDecimal()
        private val priorityFee = 0.0003.toBigDecimal()
    }
}