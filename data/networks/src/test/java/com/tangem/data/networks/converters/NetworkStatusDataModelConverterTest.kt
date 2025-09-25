package com.tangem.data.networks.converters

import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency.ID
import com.tangem.domain.models.currency.CryptoCurrency.ID.Body
import com.tangem.domain.models.currency.CryptoCurrency.ID.Prefix
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.network.NetworkStatus.Amount
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NetworkStatusDataModelConverterTest {

    private val network: Network = MockCryptoCurrencyFactory().ethereum.network

    @ParameterizedTest
    @MethodSource("provideTestModels")
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
                        ID(
                            prefix = Prefix.COIN_PREFIX,
                            body = Body.NetworkId(rawId = "BCH"),
                            suffix = ID.Suffix.RawID(rawId = "bitcoin-cash"),
                        ) to Amount.Loaded(value = BigDecimal.ZERO),
                        ID(
                            prefix = Prefix.COIN_PREFIX,
                            body = Body.NetworkId(rawId = "BTC"),
                            suffix = ID.Suffix.RawID(rawId = "bitcoin"),
                        ) to Amount.NotFound,
                    ),
                    pendingTransactions = mapOf(), // doesn't matter
                    yieldSupplyStatuses = mapOf(
                        ID(
                            prefix = Prefix.COIN_PREFIX,
                            body = Body.NetworkId(rawId = "BCH"),
                            suffix = ID.Suffix.RawID(rawId = "bitcoin-cash"),
                        ) to YieldSupplyStatus(
                            isActive = false,
                            isInitialized = false,
                            isAllowedToSpend = false,
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
            expected = NetworkStatusDM.Verified(
                networkId = NetworkStatusDM.ID(network.rawId),
                derivationPath = NetworkStatusDM.DerivationPath(
                    value = "",
                    type = NetworkStatusDM.DerivationPath.Type.NONE,
                ),
                selectedAddress = "0x123",
                availableAddresses = setOf(
                    NetworkStatusDM.Address(
                        value = "0x123",
                        type = NetworkStatusDM.Address.Type.Primary,
                    ),
                ),
                amounts = mapOf("coin⟨BCH⟩bitcoin-cash" to BigDecimal.ZERO),
                yieldSupplyStatuses = mapOf(
                    "coin⟨BCH⟩bitcoin-cash" to NetworkStatusDM.YieldSupplyStatus(
                        isActive = false,
                        isInitialized = false,
                        isAllowedToSpend = false,
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
            expected = NetworkStatusDM.NoAccount(
                networkId = NetworkStatusDM.ID(network.rawId),
                derivationPath = NetworkStatusDM.DerivationPath(
                    value = "",
                    type = NetworkStatusDM.DerivationPath.Type.NONE,
                ),
                selectedAddress = "0x123",
                availableAddresses = setOf(
                    NetworkStatusDM.Address(
                        value = "0x123",
                        type = NetworkStatusDM.Address.Type.Primary,
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