package com.tangem.features.send

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import java.math.BigDecimal

/**
 * Builds a [TestingCoroutineDispatcherProvider] backed by a single [StandardTestDispatcher] wired to this scope's
 * [TestScope.testScheduler], so `advanceUntilIdle()` drives all five dispatcher roles. Use in `Model`-layer tests
 * instead of copying the wiring per file.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.testDispatcherProvider(): TestingCoroutineDispatcherProvider {
    val testDispatcher = StandardTestDispatcher(testScheduler)
    return TestingCoroutineDispatcherProvider(
        main = testDispatcher,
        mainImmediate = testDispatcher,
        io = testDispatcher,
        default = testDispatcher,
        single = testDispatcher,
    )
}

/**
 * Shared `Loaded` status fixture for send-impl tests. Only [currency], [fiatRate] and [balance] differ between
 * call sites; the rest is incidental and never asserted.
 */
internal fun loadedStatus(
    currency: CryptoCurrency,
    fiatRate: BigDecimal = BigDecimal.ONE,
    balance: BigDecimal = BigDecimal.ONE,
): CryptoCurrencyStatus = CryptoCurrencyStatus(
    currency = currency,
    value = CryptoCurrencyStatus.Loaded(
        amount = balance,
        fiatAmount = fiatRate,
        fiatRate = fiatRate,
        priceChange = BigDecimal.ZERO,
        stakingBalance = null,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = NetworkAddress.Single(
            NetworkAddress.Address(value = "address", type = NetworkAddress.Address.Type.Primary),
        ),
        sources = CryptoCurrencyStatus.Sources(),
    ),
)

/** Throwaway [Fee.Common] for tests that only need "some fee" of a non-special type. */
internal fun commonFee(blockchain: Blockchain = Blockchain.Ethereum): Fee.Common = Fee.Common(Amount(blockchain))