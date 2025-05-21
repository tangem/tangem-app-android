package com.tangem.data.networks.converters

import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency.ID
import com.tangem.domain.models.currency.CryptoCurrency.ID.Body
import com.tangem.domain.models.currency.CryptoCurrency.ID.Prefix
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.network.NetworkStatus.Amount
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SimpleNetworkStatusConverterTest {

    private val network: Network = MockCryptoCurrencyFactory().ethereum.network

    @ParameterizedTest
    @MethodSource("provideTestModels")
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
            value = NetworkStatusDM.Verified(
                networkId = NetworkStatusDM.ID(network.rawId),
                derivationPath = NetworkStatusDM.DerivationPath(
                    value = "card",
                    type = NetworkStatusDM.DerivationPath.Type.CARD,
                ),
                selectedAddress = "0x1",
                availableAddresses = setOf(
                    NetworkStatusDM.Address(
                        value = "0x1",
                        type = NetworkStatusDM.Address.Type.Primary,
                    ),
                    NetworkStatusDM.Address(
                        value = "0x2",
                        type = NetworkStatusDM.Address.Type.Secondary,
                    ),
                ),
                amounts = mapOf(
                    "coin⟨BCH⟩bitcoin-cash" to BigDecimal.ZERO,
                    "coin⟨ETH→12367123⟩ethereum" to BigDecimal.ONE,
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
                        ID(
                            prefix = Prefix.COIN_PREFIX,
                            body = Body.NetworkId(rawId = "BCH"),
                            suffix = ID.Suffix.RawID(rawId = "bitcoin-cash"),
                        ) to Amount.Loaded(value = BigDecimal.ZERO),
                        ID(
                            prefix = Prefix.COIN_PREFIX,
                            body = Body.NetworkIdWithDerivationPath(rawId = "ETH", derivationPathHashCode = 12367123),
                            suffix = ID.Suffix.RawID(rawId = "ethereum"),
                        ) to Amount.Loaded(value = BigDecimal.ONE),
                    ),
                    pendingTransactions = emptyMap(),
                    source = StatusSource.CACHE,
                ),
            ).let(Result.Companion::success),
        ),
        // endregion

        // region NoAccount
        ConvertModel(
            value = NetworkStatusDM.NoAccount(
                networkId = NetworkStatusDM.ID(network.rawId),
                derivationPath = NetworkStatusDM.DerivationPath(
                    value = "card",
                    type = NetworkStatusDM.DerivationPath.Type.CARD,
                ),
                selectedAddress = "0x1",
                availableAddresses = setOf(
                    NetworkStatusDM.Address(
                        value = "0x1",
                        type = NetworkStatusDM.Address.Type.Primary,
                    ),
                    NetworkStatusDM.Address(
                        value = "0x2",
                        type = NetworkStatusDM.Address.Type.Secondary,
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
            value = NetworkStatusDM.Verified(
                networkId = NetworkStatusDM.ID(network.rawId),
                derivationPath = NetworkStatusDM.DerivationPath(
                    value = "card",
                    type = NetworkStatusDM.DerivationPath.Type.CARD,
                ),
                selectedAddress = "0x1",
                availableAddresses = setOf(
                    NetworkStatusDM.Address(
                        value = "0x2",
                        type = NetworkStatusDM.Address.Type.Primary,
                    ),
                ),
                amounts = emptyMap(),
            ),
            expected = Result.failure(
                exception = IllegalArgumentException("Selected address must not be null"),
            ),
        ),
        ConvertModel(
            value = NetworkStatusDM.Verified(
                networkId = NetworkStatusDM.ID(network.rawId),
                derivationPath = NetworkStatusDM.DerivationPath(
                    value = "card",
                    type = NetworkStatusDM.DerivationPath.Type.CARD,
                ),
                selectedAddress = "0x1",
                availableAddresses = setOf(),
                amounts = emptyMap(),
            ),
            expected = Result.failure(
                exception = IllegalArgumentException("Selected address must not be null"),
            ),
        ),
        ConvertModel(
            value = NetworkStatusDM.Verified(
                networkId = NetworkStatusDM.ID(network.rawId),
                derivationPath = NetworkStatusDM.DerivationPath(
                    value = "card",
                    type = NetworkStatusDM.DerivationPath.Type.CARD,
                ),
                selectedAddress = "",
                availableAddresses = setOf(
                    NetworkStatusDM.Address(
                        value = "0x1",
                        type = NetworkStatusDM.Address.Type.Primary,
                    ),
                    NetworkStatusDM.Address(
                        value = "0x2",
                        type = NetworkStatusDM.Address.Type.Secondary,
                    ),
                ),
                amounts = emptyMap(),
            ),
            expected = Result.failure(
                exception = IllegalArgumentException("Selected address must not be null"),
            ),
        ),
        ConvertModel(
            value = NetworkStatusDM.NoAccount(
                networkId = NetworkStatusDM.ID(network.rawId),
                derivationPath = NetworkStatusDM.DerivationPath(
                    value = "card",
                    type = NetworkStatusDM.DerivationPath.Type.CARD,
                ),
                selectedAddress = "",
                availableAddresses = setOf(
                    NetworkStatusDM.Address(
                        value = "0x1",
                        type = NetworkStatusDM.Address.Type.Primary,
                    ),
                    NetworkStatusDM.Address(
                        value = "0x2",
                        type = NetworkStatusDM.Address.Type.Secondary,
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
            value = NetworkStatusDM.NoAccount(
                networkId = NetworkStatusDM.ID(network.rawId),
                derivationPath = NetworkStatusDM.DerivationPath(
                    value = "card",
                    type = NetworkStatusDM.DerivationPath.Type.CARD,
                ),
                selectedAddress = "0x1",
                availableAddresses = setOf(
                    NetworkStatusDM.Address(
                        value = "0x2",
                        type = NetworkStatusDM.Address.Type.Primary,
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
            value = NetworkStatusDM.NoAccount(
                networkId = NetworkStatusDM.ID(network.rawId),
                derivationPath = NetworkStatusDM.DerivationPath(
                    value = "card",
                    type = NetworkStatusDM.DerivationPath.Type.CARD,
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