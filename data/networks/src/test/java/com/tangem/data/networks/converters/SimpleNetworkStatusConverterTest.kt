package com.tangem.data.networks.converters

import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.datasource.local.network.entity.NetworkStatusDM.*
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency.ID
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.network.NetworkStatus.Amount
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SimpleNetworkStatusConverterTest {

    private val network: Network = MockCryptoCurrencyFactory().ethereum.network

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = runCatching { SimpleNetworkStatusConverter.convert(value = model.value) }

        // Assert
        actual
            .onSuccess {
                Truth.assertThat(it).isEqualTo(model.expected.getOrNull())
            }
            .onFailure {
                val expected = model.expected.exceptionOrNull()!!

                Truth.assertThat(it).isInstanceOf(expected::class.java)
                Truth.assertThat(it).hasMessageThat().isEqualTo(expected.message)
            }
    }

    private fun provideTestModels() = listOf(
        // region Verified
        ConvertModel(
            value = Verified(
                networkId = ID(network.rawId),
                derivationPath = DerivationPath(
                    value = "card",
                    type = DerivationPath.Type.CARD,
                ),
                selectedAddress = "0x1",
                availableAddresses = setOf(
                    Address(value = "0x1", type = Address.Type.Primary),
                    Address(value = "0x2", type = Address.Type.Secondary),
                ),
                amounts = listOf(
                    CurrencyAmount(CurrencyId.createCoinId("ethereum"), BigDecimal.ZERO),
                    CurrencyAmount(CurrencyId.createTokenId("usdt", "0x1"), BigDecimal.ZERO),
                ),
                yieldSupplyStatuses = listOf(
                    YieldSupplyStatus(
                        id = CurrencyId.createCoinId("ethereum"),
                        isActive = false,
                        isInitialized = false,
                        isAllowedToSpend = false,
                        effectiveProtocolBalance = BigDecimal.ONE,
                    ),
                ),
            ),
            expected = SimpleNetworkStatus(
                id = Network.ID(
                    value = network.rawId,
                    derivationPath = Network.DerivationPath.Card("card"),
                ),
                value = NetworkStatus.Verified(
                    address = NetworkAddress.Selectable(
                        defaultAddress = NetworkAddress.Address(
                            value = "0x1",
                            type = NetworkAddress.Address.Type.Primary,
                        ),
                        availableAddresses = setOf(
                            NetworkAddress.Address(
                                value = "0x2",
                                type = NetworkAddress.Address.Type.Secondary,
                            ),
                            NetworkAddress.Address(
                                value = "0x1",
                                type = NetworkAddress.Address.Type.Primary,
                            ),
                        ),
                    ),
                    amounts = mapOf(
                        ID.fromValue("coin⟨ETH→3046160⟩ethereum") to Amount.Loaded(value = BigDecimal.ZERO),
                        ID.fromValue("token⟨ETH→3046160⟩usdt⚓0x1") to Amount.Loaded(value = BigDecimal.ZERO),
                    ),
                    pendingTransactions = emptyMap(),
                    yieldSupplyStatuses = mapOf(
                        ID.fromValue("coin⟨ETH→3046160⟩ethereum") to YieldSupplyStatus(
                            isActive = false,
                            isInitialized = false,
                            isAllowedToSpend = false,
                            effectiveProtocolBalance = BigDecimal.ONE,
                        ),
                    ),
                    source = StatusSource.CACHE,
                ),
            ).let(Result.Companion::success),
        ),
        // endregion

        // region NoAccount
        ConvertModel(
            value = NoAccount(
                networkId = ID(network.rawId),
                derivationPath = DerivationPath(
                    value = "card",
                    type = DerivationPath.Type.CARD,
                ),
                selectedAddress = "0x1",
                availableAddresses = setOf(
                    Address(
                        value = "0x1",
                        type = Address.Type.Primary,
                    ),
                    Address(
                        value = "0x2",
                        type = Address.Type.Secondary,
                    ),
                ),
                amountToCreateAccount = BigDecimal.ONE,
                errorMessage = "errorMessage",
            ),
            expected = SimpleNetworkStatus(
                id = Network.ID(
                    value = network.rawId,
                    derivationPath = Network.DerivationPath.Card("card"),
                ),
                value = NetworkStatus.NoAccount(
                    address = NetworkAddress.Selectable(
                        defaultAddress = NetworkAddress.Address(
                            value = "0x1",
                            type = NetworkAddress.Address.Type.Primary,
                        ),
                        availableAddresses = setOf(
                            NetworkAddress.Address(
                                value = "0x2",
                                type = NetworkAddress.Address.Type.Secondary,
                            ),
                            NetworkAddress.Address(
                                value = "0x1",
                                type = NetworkAddress.Address.Type.Primary,
                            ),
                        ),
                    ),
                    amountToCreateAccount = BigDecimal.ONE,
                    errorMessage = "errorMessage",
                    source = StatusSource.CACHE,
                ),
            ).let(Result.Companion::success),
        ),
        // endregion

        // region Error
        ConvertModel(
            value = Verified(
                networkId = ID(network.rawId),
                derivationPath = DerivationPath(
                    value = "card",
                    type = DerivationPath.Type.CARD,
                ),
                selectedAddress = "0x1",
                availableAddresses = setOf(
                    Address(
                        value = "0x2",
                        type = Address.Type.Primary,
                    ),
                ),
                amounts = emptyList(),
                yieldSupplyStatuses = emptyList(),
            ),
            expected = Result.failure(
                exception = IllegalArgumentException("Selected address must not be null"),
            ),
        ),
        ConvertModel(
            value = Verified(
                networkId = ID(network.rawId),
                derivationPath = DerivationPath(
                    value = "card",
                    type = DerivationPath.Type.CARD,
                ),
                selectedAddress = "0x1",
                availableAddresses = setOf(),
                amounts = emptyList(),
                yieldSupplyStatuses = emptyList(),
            ),
            expected = Result.failure(
                exception = IllegalArgumentException("Selected address must not be null"),
            ),
        ),
        ConvertModel(
            value = Verified(
                networkId = ID(network.rawId),
                derivationPath = DerivationPath(
                    value = "card",
                    type = DerivationPath.Type.CARD,
                ),
                selectedAddress = "",
                availableAddresses = setOf(
                    Address(
                        value = "0x1",
                        type = Address.Type.Primary,
                    ),
                    Address(
                        value = "0x2",
                        type = Address.Type.Secondary,
                    ),
                ),
                amounts = emptyList(),
                yieldSupplyStatuses = emptyList(),
            ),
            expected = Result.failure(
                exception = IllegalArgumentException("Selected address must not be null"),
            ),
        ),
        ConvertModel(
            value = NoAccount(
                networkId = ID(network.rawId),
                derivationPath = DerivationPath(
                    value = "card",
                    type = DerivationPath.Type.CARD,
                ),
                selectedAddress = "",
                availableAddresses = setOf(
                    Address(
                        value = "0x1",
                        type = Address.Type.Primary,
                    ),
                    Address(
                        value = "0x2",
                        type = Address.Type.Secondary,
                    ),
                ),
                amountToCreateAccount = BigDecimal.ONE,
                errorMessage = "errorMessage",
            ),
            expected = Result.failure(
                exception = IllegalArgumentException("Selected address must not be null"),
            ),
        ),
        ConvertModel(
            value = NoAccount(
                networkId = ID(network.rawId),
                derivationPath = DerivationPath(
                    value = "card",
                    type = DerivationPath.Type.CARD,
                ),
                selectedAddress = "0x1",
                availableAddresses = setOf(
                    Address(
                        value = "0x2",
                        type = Address.Type.Primary,
                    ),
                ),
                amountToCreateAccount = BigDecimal.ONE,
                errorMessage = "errorMessage",
            ),
            expected = Result.failure(
                exception = IllegalArgumentException("Selected address must not be null"),
            ),
        ),
        ConvertModel(
            value = NoAccount(
                networkId = ID(network.rawId),
                derivationPath = DerivationPath(
                    value = "card",
                    type = DerivationPath.Type.CARD,
                ),
                selectedAddress = "0x1",
                availableAddresses = setOf(),
                amountToCreateAccount = BigDecimal.ONE,
                errorMessage = "errorMessage",
            ),
            expected = Result.failure(
                exception = IllegalArgumentException("Selected address must not be null"),
            ),
        ),
        // endregion
    )

    data class ConvertModel(val value: NetworkStatusDM, val expected: Result<SimpleNetworkStatus>)
}