package com.tangem.data.networks.utils

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.*
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.walletmanager.model.Address
import com.tangem.domain.walletmanager.model.CryptoCurrencyAmount
import com.tangem.domain.walletmanager.model.CryptoCurrencyTransaction
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@RunWith(Parameterized::class)
internal class NetworkStatusFactoryTest(private val model: Model) {

    @Test
    fun test() {
        val actual = runCatching {
            NetworkStatusFactory.create(
                network = model.network,
                updatingResult = model.result,
                addedCurrencies = model.currencies,
            )
        }

        actual
            .onSuccess {
                Truth.assertThat(actual).isEqualTo(model.expected)
            }
            .onFailure {
                Truth.assertThat(actual.exceptionOrNull()).isInstanceOf(it::class.java)
                Truth.assertThat(actual.exceptionOrNull()).hasMessageThat().isEqualTo(it.message)
            }
    }

    data class Model(
        val network: Network,
        val result: UpdateWalletManagerResult,
        val currencies: Set<CryptoCurrency>,
        val expected: Result<NetworkStatus>,
    )

    private companion object {

        val selectedAddressThrowable = IllegalArgumentException("Selected address must not be null")

        val currencies = with(MockCryptoCurrencyFactory()) { setOf(ethereum, createToken(Blockchain.Ethereum)) }

        val txHistoryItem = TxHistoryItem(
            txHash = "erroribus",
            timestampInMillis = 2771,
            isOutgoing = false,
            destinationType = TxHistoryItem.DestinationType.Single(
                addressType = TxHistoryItem.AddressType.User("0x1"),
            ),
            sourceType = TxHistoryItem.SourceType.Single("0x2"),
            interactionAddressType = null,
            status = TxHistoryItem.TransactionStatus.Confirmed,
            type = TxHistoryItem.TransactionType.Transfer,
            amount = BigDecimal.ONE,
        )

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Model> = listOf(
            // region MissedDerivation
            createSuccess(
                result = UpdateWalletManagerResult.MissedDerivation,
                status = NetworkStatus.MissedDerivation,
            ),
            // endregion

            // region Unreachable
            createSuccess(
                result = UpdateWalletManagerResult.Unreachable(selectedAddress = null, addresses = null),
                status = NetworkStatus.Unreachable(address = null),
            ),
            createSuccess(
                result = UpdateWalletManagerResult.Unreachable(
                    selectedAddress = "",
                    addresses = emptySet(),
                ),
                status = NetworkStatus.Unreachable(address = null),
            ),
            createSuccess(
                result = UpdateWalletManagerResult.Unreachable(
                    selectedAddress = "",
                    addresses = setOf(Address(value = "", type = Address.Type.Primary)),
                ),
                status = NetworkStatus.Unreachable(address = null),
            ),
            createSuccess(
                result = UpdateWalletManagerResult.Unreachable(
                    selectedAddress = "0x1",
                    addresses = setOf(Address(value = "0x1", type = Address.Type.Primary)),
                ),
                status = NetworkStatus.Unreachable(
                    address = NetworkAddress.Single(
                        defaultAddress = NetworkAddress.Address(
                            value = "0x1",
                            type = NetworkAddress.Address.Type.Primary,
                        ),
                    ),
                ),
            ),
            createFailure(
                result = UpdateWalletManagerResult.Unreachable(
                    selectedAddress = "0x1",
                    addresses = emptySet(),
                ),
                throwable = selectedAddressThrowable,
            ),
            // endregion

            // region NoAccount
            createFailure(
                result = UpdateWalletManagerResult.NoAccount(
                    selectedAddress = "",
                    addresses = emptySet(),
                    amountToCreateAccount = BigDecimal.ZERO,
                    errorMessage = "",
                ),
                throwable = selectedAddressThrowable,
            ),
            createFailure(
                result = UpdateWalletManagerResult.NoAccount(
                    selectedAddress = "",
                    addresses = setOf(Address(value = "", type = Address.Type.Primary)),
                    amountToCreateAccount = BigDecimal.ZERO,
                    errorMessage = "",
                ),
                throwable = selectedAddressThrowable,
            ),
            createFailure(
                result = UpdateWalletManagerResult.NoAccount(
                    selectedAddress = "0x1",
                    addresses = setOf(Address(value = "0x2", type = Address.Type.Primary)),
                    amountToCreateAccount = BigDecimal.ZERO,
                    errorMessage = "",
                ),
                throwable = selectedAddressThrowable,
            ),
            createSuccess(
                result = UpdateWalletManagerResult.NoAccount(
                    selectedAddress = "0x1",
                    addresses = setOf(Address(value = "0x1", type = Address.Type.Primary)),
                    amountToCreateAccount = BigDecimal.ZERO,
                    errorMessage = "",
                ),
                status = NetworkStatus.NoAccount(
                    address = NetworkAddress.Single(
                        defaultAddress = NetworkAddress.Address(
                            value = "0x1",
                            type = NetworkAddress.Address.Type.Primary,
                        ),
                    ),
                    amountToCreateAccount = BigDecimal.ZERO,
                    errorMessage = "",
                    source = StatusSource.ACTUAL,
                ),
            ),
            // endregion

            // region Verified
            createFailure(
                result = UpdateWalletManagerResult.Verified(
                    selectedAddress = "",
                    addresses = emptySet(),
                    currenciesAmounts = emptySet(),
                    currentTransactions = emptySet(),
                ),
                throwable = selectedAddressThrowable,
            ),
            createFailure(
                result = UpdateWalletManagerResult.Verified(
                    selectedAddress = "",
                    addresses = setOf(Address(value = "", type = Address.Type.Primary)),
                    currenciesAmounts = emptySet(),
                    currentTransactions = emptySet(),
                ),
                throwable = selectedAddressThrowable,
            ),
            createFailure(
                result = UpdateWalletManagerResult.Verified(
                    selectedAddress = "0x1",
                    addresses = setOf(Address(value = "0x2", type = Address.Type.Primary)),
                    currenciesAmounts = emptySet(),
                    currentTransactions = emptySet(),
                ),
                throwable = selectedAddressThrowable,
            ),
            createFailure(
                result = UpdateWalletManagerResult.Verified(
                    selectedAddress = "0x1",
                    addresses = setOf(Address(value = "0x2", type = Address.Type.Primary)),
                    currenciesAmounts = emptySet(),
                    currentTransactions = emptySet(),
                ),
                throwable = selectedAddressThrowable,
            ),
            createSuccess(
                result = UpdateWalletManagerResult.Verified(
                    selectedAddress = "0x1",
                    addresses = setOf(Address(value = "0x1", type = Address.Type.Primary)),
                    currenciesAmounts = setOf(
                        CryptoCurrencyAmount.Coin(value = BigDecimal.ONE),
                    ),
                    currentTransactions = setOf(
                        CryptoCurrencyTransaction.Coin(txHistoryItem),
                    ),
                ),
                currencies = currencies,
                status = NetworkStatus.Verified(
                    address = NetworkAddress.Single(
                        defaultAddress = NetworkAddress.Address(
                            value = "0x1",
                            type = NetworkAddress.Address.Type.Primary,
                        ),
                    ),
                    amounts = mapOf(
                        currencies.first().id to CryptoCurrencyAmountStatus.Loaded(BigDecimal.ONE),
                        currencies.last().id to CryptoCurrencyAmountStatus.NotFound,
                    ),
                    pendingTransactions = mapOf(
                        currencies.first().id to setOf(txHistoryItem),
                        currencies.last().id to setOf(),
                    ),
                    source = StatusSource.ACTUAL,
                ),
            ),
            // endregion
        )

        fun createSuccess(
            result: UpdateWalletManagerResult,
            currencies: Set<CryptoCurrency> = emptySet(),
            status: NetworkStatus.Value,
        ): Model {
            val network = MockCryptoCurrencyFactory().ethereum.network

            return Model(
                network = network,
                result = result,
                currencies = currencies,
                expected = Result.success(
                    value = NetworkStatus(network = network, value = status),
                ),
            )
        }

        fun createFailure(
            result: UpdateWalletManagerResult,
            currencies: Set<CryptoCurrency> = emptySet(),
            throwable: Throwable,
        ): Model {
            val network = MockCryptoCurrencyFactory().ethereum.network

            return Model(
                network = network,
                result = result,
                currencies = currencies,
                expected = Result.failure(throwable),
            )
        }
    }
}