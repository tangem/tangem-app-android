package com.tangem.tap.features.wallet.ui.test

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.common.TestAction
import com.tangem.tap.common.TestActions
import com.tangem.tap.domain.tokens.BlockchainNetwork
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import java.math.BigDecimal
import kotlin.random.Random

/**
[REDACTED_AUTHOR]
 */
class TestWalletDetails {
    companion object {
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
        val currency = store.state.walletState.getSelectedWalletData()!!.currency
        return BlockchainNetwork(currency.blockchain, currency.derivationPath, listOf())
    }
}