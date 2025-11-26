package com.tangem.data.networks.converters

import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.datasource.local.network.entity.NetworkStatusDM.*
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency.ID
import com.tangem.domain.models.currency.CryptoCurrency.ID.Body
import com.tangem.domain.models.currency.CryptoCurrency.ID.Prefix
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.network.NetworkStatus.Amount
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NetworkStatusDataModelConverterTest {

    private val network: Network = MockCryptoCurrencyFactory().ethereum.network

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = NetworkStatusDataModelConverter.convert(value = model.value)

        // Assert
        Truth.assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        // region Verified
        ConvertModel(
            value = NetworkStatus(
                network = network,
                value = NetworkStatus.Verified(
                    address = NetworkAddress.Single(
                        defaultAddress = NetworkAddress.Address(
                            value = "0x123",
                            type = NetworkAddress.Address.Type.Primary,
                        ),
                    ),
                    amounts = mapOf(
                        ID.fromValue(value = "coin⟨ETH→0⟩ethereum") to Amount.Loaded(value = BigDecimal.ZERO),
                        ID(
                            prefix = Prefix.COIN_PREFIX,
                            body = Body.NetworkId(rawId = "BTC"),
                            suffix = ID.Suffix.RawID(rawId = "bitcoin"),
                        ) to Amount.NotFound,
                    ),
                    pendingTransactions = mapOf(), // doesn't matter
                    yieldSupplyStatuses = mapOf(
                        ID.fromValue(value = "token⟨ETH→0⟩usdt⚓0x1") to YieldSupplyStatus(
                            isActive = false,
                            isInitialized = false,
                            isAllowedToSpend = false,
                            effectiveProtocolBalance = BigDecimal.ONE,
                        ),
                        ID(
                            prefix = Prefix.COIN_PREFIX,
                            body = Body.NetworkId(rawId = "BTC"),
                            suffix = ID.Suffix.RawID(rawId = "bitcoin"),
                        ) to null,
                    ),
                    source = StatusSource.ACTUAL, // doesn't matter
                ),
            ),
            expected = Verified(
                networkId = ID(network.rawId),
                derivationPath = DerivationPath(
                    value = "",
                    type = DerivationPath.Type.NONE,
                ),
                selectedAddress = "0x123",
                availableAddresses = setOf(
                    Address(
                        value = "0x123",
                        type = Address.Type.Primary,
                    ),
                ),
                amounts = listOf(
                    CurrencyAmount(CurrencyId.createCoinId("ethereum"), BigDecimal.ZERO),
                ),
                yieldSupplyStatuses = listOf(
                    YieldSupplyStatus(
                        id = CurrencyId.createTokenId("usdt", "0x1"),
                        isActive = false,
                        isInitialized = false,
                        isAllowedToSpend = false,
                        effectiveProtocolBalance = BigDecimal.ONE,
                    ),
                ),
            ),
        ),
        // endregion

        // region NoAccount
        ConvertModel(
            value = NetworkStatus(
                network = network,
                value = NetworkStatus.NoAccount(
                    address = NetworkAddress.Single(
                        defaultAddress = NetworkAddress.Address(
                            value = "0x123",
                            type = NetworkAddress.Address.Type.Primary,
                        ),
                    ),
                    amountToCreateAccount = BigDecimal.ONE,
                    errorMessage = "errorMessage",
                    source = StatusSource.ACTUAL, // doesn't matter
                ),
            ),
            expected = NoAccount(
                networkId = ID(network.rawId),
                derivationPath = DerivationPath(
                    value = "",
                    type = DerivationPath.Type.NONE,
                ),
                selectedAddress = "0x123",
                availableAddresses = setOf(
                    Address(
                        value = "0x123",
                        type = Address.Type.Primary,
                    ),
                ),
                amountToCreateAccount = BigDecimal.ONE,
                errorMessage = "errorMessage",
            ),
        ),
        // endregion

        // region Other NetworkStatus
        ConvertModel(
            value = NetworkStatus(network = network, value = NetworkStatus.Unreachable(address = null)),
            expected = null,
        ),
        ConvertModel(
            value = NetworkStatus(
                network = network,
                value = NetworkStatus.Unreachable(
                    address = NetworkAddress.Single(
                        defaultAddress = NetworkAddress.Address(
                            value = "0x123",
                            type = NetworkAddress.Address.Type.Primary,
                        ),
                    ),
                ),
            ),
            expected = null,
        ),
        ConvertModel(
            value = NetworkStatus(network = network, value = NetworkStatus.MissedDerivation),
            expected = null,
        ),
        // endregion
    )

    data class ConvertModel(val value: NetworkStatus, val expected: NetworkStatusDM?)
}