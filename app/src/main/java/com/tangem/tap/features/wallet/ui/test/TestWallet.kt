package com.tangem.tap.features.wallet.ui.test

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.extensions.guard
import com.tangem.tap.common.TestAction
import com.tangem.tap.common.TestActions
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import java.math.BigDecimal
import kotlin.random.Random

/**
 * Created by Anton Zhilenkov on 19/04/2022.
 */
object TestWallet {
    fun solanaRentExemptWarning(): List<TestAction> {
        val checker = SolanaRentWarningActionEmitter()
        return listOf(
            "BALANCE = 0.0" to { checker.setZeroBalance() },
            "BALANCE < 0.00089088" to { checker.setLessThanRentExempt() },
            "BALANCE > 0.00089088" to { checker.setMoreThanRentExempt() },
            "BALANCE = 0.00089087" to { checker.setLessThanRentExemptByOne() },
            "BALANCE = 0.00089088 (rent exempt)" to { checker.setForRentExemptBarrier() },
            "BALANCE = 0.00089089" to { checker.setMoreThanRentExemptByOne() },
        )
    }

    fun getBlockchainBalanceActions(blockchainNetwork: BlockchainNetwork): List<TestAction> {
        return getBlockchainBalanceActions(
            getWalletManager(blockchainNetwork),
            blockchainNetwork.blockchain.decimals(),
        )
    }

    fun getTokenBalanceAction(blockchainNetwork: BlockchainNetwork, token: Token): List<TestAction> {
        return getTokenBalanceAction(getWalletManager(blockchainNetwork), token)
    }

    fun getBlockchainBalanceActions(walletManager: WalletManager?, decimals: Int): List<TestAction> {
        val minValue = BigDecimal.ONE.movePointLeft(decimals)
        val averageValue = BigDecimal.ONE.movePointRight(decimals)
            .divide(BigDecimal(2)).movePointLeft(decimals)
        val maxValue = BigDecimal(2).pow(32)

        return listOf(
            "0.0" to { setBalance(walletManager, BigDecimal.ZERO) },
            "Min value" to { setBalance(walletManager, minValue) },
            "Average value" to { setBalance(walletManager, averageValue) },
            "Max value" to { setBalance(walletManager, maxValue) },
            "1234567890123.01234567890123" to { setBalance(walletManager, BigDecimal.ZERO) },
        )
    }

    fun getTokenBalanceAction(walletManager: WalletManager?, token: Token): List<TestAction> {
        val minValue = BigDecimal.ONE.movePointLeft(token.decimals)
        val averageValue = BigDecimal.ONE.movePointRight(token.decimals)
            .divide(BigDecimal(2)).movePointLeft(token.decimals)
        val maxValue = BigDecimal(2).pow(32)

        return listOf(
            "0.0" to { setTokenBalance(walletManager, BigDecimal.ZERO, token) },
            "Min value" to { setTokenBalance(walletManager, minValue, token) },
            "Average value" to { setTokenBalance(walletManager, averageValue, token) },
            "Max value" to { setTokenBalance(walletManager, maxValue, token) },
            "1234567890123.01234567890123" to { setTokenBalance(walletManager, BigDecimal.ZERO, token) },
        )
    }

    fun setBalance(walletManager: WalletManager?, value: BigDecimal) {
        val manager = walletManager.guard {
            store.dispatchDebugErrorNotification("WalletManager not found")
            return
        }
        val amount = manager.wallet.amounts[AmountType.Coin] ?: Amount(manager.wallet.blockchain)
        setBalance(manager, amount, value)
    }

    private fun setTokenBalance(walletManager: WalletManager?, value: BigDecimal, token: Token? = null) {
        val manager = walletManager.guard {
            store.dispatchDebugErrorNotification("WalletManager not found")
            return
        }
        val token = token.guard {
            store.dispatchDebugErrorNotification("Token not found")
            return
        }

        val amount = manager.wallet.amounts[AmountType.Token(token)] ?: Amount(token)
        setBalance(manager, amount, value)
    }

    private fun setBalance(walletManager: WalletManager, amount: Amount, value: BigDecimal) {
        TestActions.testAmountInjectionForWalletManagerEnabled = true
        walletManager.wallet.setAmount(amount.copy(value = value))
        store.dispatch(WalletAction.LoadData)
    }

    private fun getWalletManager(blockchainNetwork: BlockchainNetwork): WalletManager? {
        return store.state.walletState.getWalletManager(blockchainNetwork)
    }
}

private class SolanaRentWarningActionEmitter {

    private val zero = BigDecimal.ZERO
    private val one = BigDecimal(0.00000001)
    private val rentExemptBarrier = BigDecimal(0.00089088)
    private val lessThanRentExemptByOne = rentExemptBarrier.minus(one)
    private val moreThanRentExemptByOne = rentExemptBarrier.plus(one)
    private val moreThanRentExempt = rentExemptBarrier.plus(Random.nextDouble().toBigDecimal())
    private val lessThanRentExempt = rentExemptBarrier
        .minus(Random.nextDouble(0.0, rentExemptBarrier.minus(one).toDouble()).toBigDecimal())

    fun setZeroBalance() {
        setBalance(zero)
    }

    fun setForRentExemptBarrier() {
        setBalance(rentExemptBarrier)
    }

    fun setLessThanRentExemptByOne() {
        setBalance(lessThanRentExemptByOne)
    }

    fun setMoreThanRentExemptByOne() {
        setBalance(moreThanRentExemptByOne)
    }

    fun setMoreThanRentExempt() {
        setBalance(moreThanRentExempt)
    }

    fun setLessThanRentExempt() {
        setBalance(lessThanRentExempt)
    }

    private fun setBalance(value: BigDecimal) {
        val amount = Amount(getBlockchainNetwork().blockchain).copy(value = value)
        getWalletManager().apply {
            TestActions.testAmountInjectionForWalletManagerEnabled = true
            wallet.setAmount(amount)
        }
        store.dispatch(WalletAction.LoadData)
    }

    private fun getWalletManager(): WalletManager {
        return store.state.walletState.getWalletManager(getBlockchainNetwork())!!
    }

    private fun getBlockchainNetwork(): BlockchainNetwork {
        val currency = store.state.walletState.selectedWalletData!!.currency
        return BlockchainNetwork(currency.blockchain, currency.derivationPath, listOf())
    }
}
