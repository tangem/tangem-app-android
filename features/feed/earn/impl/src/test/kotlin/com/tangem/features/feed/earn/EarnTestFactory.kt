package com.tangem.features.feed.earn

import com.tangem.domain.earn.model.EarnFilter
import com.tangem.domain.earn.model.EarnFilterNetwork
import com.tangem.domain.earn.model.EarnFilterType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnError
import com.tangem.domain.models.earn.EarnNetwork
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnToken
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnType
import com.tangem.domain.models.network.Network
import io.mockk.every
import io.mockk.mockk

/**
 * Fixtures for the Earn tab tests. Every argument is defaulted so a test overrides only the fields it
 * asserts on.
 *
 * [EarnToken], [EarnNetwork] and the filters are plain data classes, so they are built for real.
 * [CryptoCurrency] is mocked: a real one needs a delimiter-encoded [CryptoCurrency.ID], which would drag
 * those delimiters into every expected row id.
 */

internal fun createEarnToken(
    apy: String = "0.055",
    networkId: String = "ethereum",
    rewardType: EarnRewardType = EarnRewardType.APY,
    type: EarnType = EarnType.STAKING,
    tokenId: String = "ethereum",
    tokenSymbol: String = "ETH",
    tokenName: String = "Ethereum",
    tokenAddress: String? = null,
    decimalCount: Int? = null,
): EarnToken = EarnToken(
    apy = apy,
    networkId = networkId,
    rewardType = rewardType,
    type = type,
    tokenId = tokenId,
    tokenSymbol = tokenSymbol,
    tokenName = tokenName,
    tokenAddress = tokenAddress,
    decimalCount = decimalCount,
)

internal fun createNetwork(
    rawId: String = "ethereum",
    name: String = "Ethereum",
    isTestnet: Boolean = false,
): Network = Network(
    id = Network.ID(value = rawId, derivationPath = Network.DerivationPath.None),
    name = name,
    currencySymbol = "ETH",
    derivationPath = Network.DerivationPath.None,
    isTestnet = isTestnet,
    standardType = Network.StandardType.ERC20,
    hasFiatFeeRate = true,
    canHandleTokens = true,
    transactionExtrasType = Network.TransactionExtrasType.NONE,
    nameResolvingType = Network.NameResolvingType.NONE,
)

internal fun createEarnCurrency(
    currencyId: String = "coin-ethereum",
    iconUrl: String? = "https://icons.tangem.com/eth.png",
    network: Network = createNetwork(),
    name: String = "Ethereum",
    symbol: String = "ETH",
    isCustom: Boolean = false,
): CryptoCurrency.Coin {
    val id: CryptoCurrency.ID = mockk {
        every { value } returns currencyId
    }
    return mockk {
        every { this@mockk.id } returns id
        every { this@mockk.network } returns network
        every { this@mockk.name } returns name
        every { this@mockk.symbol } returns symbol
        every { this@mockk.iconUrl } returns iconUrl
        every { this@mockk.isCustom } returns isCustom
    }
}

internal fun createEarnTokenWithCurrency(
    earnToken: EarnToken = createEarnToken(),
    cryptoCurrency: CryptoCurrency = createEarnCurrency(),
    networkName: String = "Ethereum",
): EarnTokenWithCurrency = EarnTokenWithCurrency(
    networkName = networkName,
    earnToken = earnToken,
    cryptoCurrency = cryptoCurrency,
)

internal fun createEarnNetwork(
    networkId: String = "ethereum",
    fullName: String = "Ethereum",
    symbol: String = "ETH",
    isAdded: Boolean = false,
): EarnNetwork = EarnNetwork(
    networkId = networkId,
    fullName = fullName,
    symbol = symbol,
    isAdded = isAdded,
)

internal fun createEarnFilter(
    network: EarnFilterNetwork = EarnFilterNetwork.AllNetworks(isSelected = true),
    type: EarnFilterType = EarnFilterType.ALL,
): EarnFilter = EarnFilter(earnFilterNetwork = network, earnFilterType = type)

/** [EarnError.HttpError.id] defaults to a random UUID, so fixtures pin it to keep instances comparable. */
internal fun createEarnHttpError(
    id: String = "error-id",
    code: Int = 500,
    message: String = "Internal error",
): EarnError = EarnError.HttpError(id = id, code = code, message = message)