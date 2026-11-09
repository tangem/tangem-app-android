package com.tangem.features.earn.yield.impl.fixtures

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import java.math.BigDecimal

private val currencyFactory: MockCryptoCurrencyFactory = MockCryptoCurrencyFactory()

internal fun coin(blockchain: Blockchain): CryptoCurrency.Coin = currencyFactory.createCoin(blockchain)

internal fun token(
    blockchain: Blockchain = Blockchain.Ethereum,
    contractAddress: String = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
): CryptoCurrency.Token = currencyFactory.createToken(
    blockchain = blockchain,
    id = "usd-coin",
    contractAddress = contractAddress,
    symbol = "USDC",
    decimals = 6,
)

internal fun yieldStatus(isActive: Boolean): YieldSupplyStatus = YieldSupplyStatus(
    isActive = isActive,
    isInitialized = true,
    isAllowedToSpend = true,
    effectiveProtocolBalance = null,
)

internal fun currencyStatus(
    currency: CryptoCurrency,
    yieldSupplyStatus: YieldSupplyStatus? = null,
    networkSource: StatusSource = StatusSource.ACTUAL,
): CryptoCurrencyStatus {
    val value = CryptoCurrencyStatus.Loaded(
        amount = BigDecimal("10"),
        fiatAmount = BigDecimal("20"),
        fiatRate = BigDecimal("2"),
        priceChange = BigDecimal.ZERO,
        yieldSupplyStatus = yieldSupplyStatus,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = NetworkAddress.Single(
            NetworkAddress.Address("address", NetworkAddress.Address.Type.Primary),
        ),
        sources = CryptoCurrencyStatus.Sources(networkSource = networkSource),
        contributions = emptyList(),
    )
    return CryptoCurrencyStatus(currency = currency, value = value)
}