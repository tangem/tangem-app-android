package com.tangem.domain.tokens.mock

import arrow.core.NonEmptySet
import arrow.core.nonEmptySetOf
import com.tangem.domain.tokens.model.CryptoCurrencyAmountStatus
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkAddress
import com.tangem.domain.tokens.model.NetworkStatus
import java.math.BigDecimal

internal object MockNetworks {

    val amountToCreateAccount: BigDecimal = BigDecimal.TEN

    val network1 = Network(
        id = Network.ID("network1"),
        name = "Network One",
        isTestnet = false,
        standardType = Network.StandardType.ERC20,
        backendId = "network1",
        currencySymbol = "ETH",
        derivationPath = Network.DerivationPath.None,
        hasFiatFeeRate = true,
        canHandleTokens = true,
        transactionExtrasType = Network.TransactionExtrasType.NONE,
    )

    val network2 = Network(
        id = Network.ID("network2"),
        name = "Network Two",
        isTestnet = false,
        standardType = Network.StandardType.ERC20,
        backendId = "network1",
        currencySymbol = "ETH",
        derivationPath = Network.DerivationPath.None,
        hasFiatFeeRate = true,
        canHandleTokens = true,
        transactionExtrasType = Network.TransactionExtrasType.NONE,
    )

    val network3 = Network(
        id = Network.ID("network3"),
        name = "Network Three",
        isTestnet = false,
        standardType = Network.StandardType.ERC20,
        backendId = "network1",
        currencySymbol = "ETH",
        derivationPath = Network.DerivationPath.None,
        hasFiatFeeRate = true,
        canHandleTokens = true,
        transactionExtrasType = Network.TransactionExtrasType.NONE,
    )

    val verifiedNetworksStatuses: NonEmptySet<NetworkStatus>
        get() = nonEmptySetOf(
            verifiedNetworkStatus1,
            verifiedNetworkStatus2,
            verifiedNetworkStatus3,
        )

    private val networkStatus1 = NetworkStatus(
        network = network1,
        value = NetworkStatus.Unreachable(address = null),
    )

    private val networkStatus2 = NetworkStatus(
        network = network2,
        value = NetworkStatus.MissedDerivation,
    )

    private val networkStatus3 = NetworkStatus(
        network = network3,
        value = NetworkStatus.NoAccount(
            amountToCreateAccount = amountToCreateAccount,
            address = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
            ),
            errorMessage = "",
            isCached = false,
        ),
    )

    private val verifiedNetworkStatus1: NetworkStatus
        get() = networkStatus1.copy(
            value = NetworkStatus.Verified(
                amounts = mapOf(
                    MockTokens.token1.id to CryptoCurrencyAmountStatus.Loaded(BigDecimal.TEN),
                    MockTokens.token2.id to CryptoCurrencyAmountStatus.Loaded(BigDecimal.TEN),
                    MockTokens.token3.id to CryptoCurrencyAmountStatus.Loaded(BigDecimal.TEN),
                ),
                pendingTransactions = emptyMap(),
                address = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
                ),
                isCached = false,
            ),
        )

    private val verifiedNetworkStatus2: NetworkStatus
        get() = networkStatus2.copy(
            value = NetworkStatus.Verified(
                amounts = mapOf(
                    MockTokens.token4.id to CryptoCurrencyAmountStatus.Loaded(BigDecimal.TEN),
                    MockTokens.token5.id to CryptoCurrencyAmountStatus.Loaded(BigDecimal.TEN),
                    MockTokens.token6.id to CryptoCurrencyAmountStatus.Loaded(BigDecimal.TEN),
                ),
                pendingTransactions = emptyMap(),
                address = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
                ),
                isCached = false,
            ),
        )

    private val verifiedNetworkStatus3: NetworkStatus
        get() = networkStatus3.copy(
            value = NetworkStatus.Verified(
                amounts = mapOf(
                    MockTokens.token7.id to CryptoCurrencyAmountStatus.Loaded(BigDecimal.TEN),
                    MockTokens.token8.id to CryptoCurrencyAmountStatus.Loaded(BigDecimal.TEN),
                    MockTokens.token9.id to CryptoCurrencyAmountStatus.Loaded(BigDecimal.TEN),
                    MockTokens.token10.id to CryptoCurrencyAmountStatus.Loaded(BigDecimal.TEN),
                ),
                pendingTransactions = emptyMap(),
                address = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
                ),
                isCached = false,
            ),
        )
}