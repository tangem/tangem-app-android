package com.tangem.features.earn.staking.impl.fixtures

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.staking.model.StakingBalance
import com.tangem.test.mock.staking.MockStaking
import java.math.BigDecimal

private val currencyFactory: MockCryptoCurrencyFactory = MockCryptoCurrencyFactory()

internal fun coin(blockchain: Blockchain): CryptoCurrency.Coin = currencyFactory.createCoin(blockchain)

internal fun polygonToken(): CryptoCurrency.Token = currencyFactory.createToken(
    blockchain = Blockchain.Ethereum,
    id = "polygon-ecosystem-token",
    contractAddress = MockStaking.POLYGON_CONTRACT,
    symbol = "POL",
    decimals = 18,
)

internal fun currencyStatus(
    currency: CryptoCurrency = coin(Blockchain.Solana),
    amount: BigDecimal? = BigDecimal("10"),
    fiatRate: BigDecimal? = BigDecimal("2"),
    stakingBalance: StakingBalance? = null,
    networkSource: StatusSource = StatusSource.ACTUAL,
): CryptoCurrencyStatus {
    val networkAddress = NetworkAddress.Single(
        NetworkAddress.Address("address", NetworkAddress.Address.Type.Primary),
    )
    val sources = CryptoCurrencyStatus.Sources(networkSource = networkSource)
    val value = when {
        amount == null -> CryptoCurrencyStatus.NoAmount(priceChange = null, fiatRate = fiatRate)
        fiatRate != null -> CryptoCurrencyStatus.Loaded(
            amount = amount,
            fiatAmount = amount * fiatRate,
            fiatRate = fiatRate,
            priceChange = BigDecimal.ZERO,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = networkAddress,
            sources = sources,
            contributions = listOfNotNull(stakingBalance),
        )
        else -> CryptoCurrencyStatus.NoQuote(
            amount = amount,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = networkAddress,
            sources = sources,
            contributions = listOfNotNull(stakingBalance),
        )
    }
    return CryptoCurrencyStatus(currency = currency, value = value)
}