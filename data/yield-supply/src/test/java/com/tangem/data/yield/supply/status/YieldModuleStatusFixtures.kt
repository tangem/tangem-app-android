package com.tangem.data.yield.supply.status

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.test.mock.yield.MockYieldSupply
import java.math.BigDecimal

internal const val USDC_ADDRESS = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"
internal const val USDT_ADDRESS = "0xdAC17F958D2ee523a2206206994597C13D831ec7"

internal val CARD_PATH = Network.DerivationPath.Card("m/44'/60'/0'/0/0")

internal fun createNetwork(
    rawId: String = "ethereum",
    derivationPath: Network.DerivationPath = CARD_PATH,
): Network = MockYieldSupply.network(id = rawId, derivationPath = derivationPath)

internal fun createToken(
    network: Network = createNetwork(),
    contractAddress: String = USDC_ADDRESS,
    symbol: String = "USDC",
): CryptoCurrency.Token = MockYieldSupply.token(contractAddress = contractAddress, network = network, symbol = symbol)

internal fun createStatus(
    token: CryptoCurrency.Token = createToken(),
    isActive: Boolean = false,
    isInitialized: Boolean = false,
    isAllowedToSpend: Boolean = false,
    effectiveProtocolBalance: BigDecimal? = null,
    source: StatusSource = StatusSource.ACTUAL,
): YieldModuleStatus {
    val key = YieldModuleStatus.Key(token)

    return YieldModuleStatus(
        networkId = key.networkId,
        tokenAddress = key.tokenAddress,
        isActive = isActive,
        isInitialized = isInitialized,
        isAllowedToSpend = isAllowedToSpend,
        effectiveProtocolBalance = effectiveProtocolBalance,
        source = source,
    )
}