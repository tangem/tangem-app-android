package com.tangem.data.networks.repository

import com.google.common.truth.Truth
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.walletmanager.MockUpdateWalletManagerResultFactory
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.networks.store.NetworksStatusesStore
import com.tangem.data.networks.store.storeStatus
import com.tangem.data.networks.utils.NetworkStatusFactory
import com.tangem.domain.models.network.CryptoCurrencyAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultNetworksRepositoryTest {

    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val networksStatusesStore: NetworksStatusesStore = mockk(relaxUnitFun = true)

    private val repository: DefaultNetworksRepository = DefaultNetworksRepository(
        cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
        walletManagersFacade = walletManagersFacade,
        networksStatusesStore = networksStatusesStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(cardCryptoCurrencyFactory, walletManagersFacade, networksStatusesStore)
    }

    @Nested
    inner class FetchPendingTransactions {

        @Test
        fun `walletManagersFacade returns Verified`() = runTest {
            // Arrange
            val result = updateWalletManagerResultFactory.createVerified()
            val status = result.toNetworkStatus()

            coEvery {
                cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network)
            } returns listOf(cryptoCurrency)

            coEvery {
                walletManagersFacade.updatePendingTransactions(userWalletId = userWalletId, network = network)
            } returns result

            coEvery { networksStatusesStore.storeStatus(userWalletId = userWalletId, status = status) } returns Unit

            // Act
            repository.fetchPendingTransactions(userWalletId = userWalletId, network = network)

            // Assert
            coVerifyOrder {
                cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network)
                walletManagersFacade.updatePendingTransactions(userWalletId = userWalletId, network = network)
                networksStatusesStore.storeStatus(userWalletId = userWalletId, status = status)
            }
        }

        @Test
        fun `walletManagersFacade returns NoAccount`() = runTest {
            // Arrange
            val result = updateWalletManagerResultFactory.createNoAccount()
            val status = result.toNetworkStatus()

            coEvery {
                cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network)
            } returns listOf(cryptoCurrency)

            coEvery {
                walletManagersFacade.updatePendingTransactions(userWalletId = userWalletId, network = network)
            } returns result

            coEvery { networksStatusesStore.storeStatus(userWalletId = userWalletId, status = status) } returns Unit

            // Act
            repository.fetchPendingTransactions(userWalletId = userWalletId, network = network)

            // Assert
            coVerifyOrder {
                cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network)
                walletManagersFacade.updatePendingTransactions(userWalletId = userWalletId, network = network)
                networksStatusesStore.storeStatus(userWalletId = userWalletId, status = status)
            }
        }

        @Test
        fun `walletManagersFacade returns MissedDerivation`() = runTest {
            // Arrange
            val result = UpdateWalletManagerResult.MissedDerivation
            val status = result.toNetworkStatus()

            coEvery {
                cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network)
            } returns listOf(cryptoCurrency)

            coEvery {
                walletManagersFacade.updatePendingTransactions(userWalletId = userWalletId, network = network)
            } returns result

            coEvery { networksStatusesStore.storeStatus(userWalletId = userWalletId, status = status) } returns Unit

            // Act
            repository.fetchPendingTransactions(userWalletId = userWalletId, network = network)

            // Assert
            coVerifyOrder {
                cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network)
                walletManagersFacade.updatePendingTransactions(userWalletId = userWalletId, network = network)
                networksStatusesStore.storeStatus(userWalletId = userWalletId, status = status)
            }
        }

        @Test
        fun `walletManagersFacade returns Unreachable`() = runTest {
            // Arrange
            val result = updateWalletManagerResultFactory.createUnreachableWithAddress()
            val status = result.toNetworkStatus()

            coEvery {
                cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network)
            } returns listOf(cryptoCurrency)

            coEvery {
                walletManagersFacade.updatePendingTransactions(userWalletId = userWalletId, network = network)
            } returns result

            coEvery { networksStatusesStore.storeStatus(userWalletId = userWalletId, status = status) } returns Unit

            // Act
            repository.fetchPendingTransactions(userWalletId = userWalletId, network = network)

            // Assert
            coVerifyOrder {
                cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network)
                walletManagersFacade.updatePendingTransactions(userWalletId = userWalletId, network = network)
                networksStatusesStore.storeStatus(userWalletId = userWalletId, status = status)
            }
        }

        @Test
        fun `cardCryptoCurrencyFactory throws exception`() = runTest {
            // Arrange
            coEvery {
                cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network)
            } throws IllegalStateException()

            // Act
            repository.fetchPendingTransactions(userWalletId = userWalletId, network = network)

            // Assert
            coVerify {
                cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network)
            }

            coVerify(inverse = true) {
                walletManagersFacade.updatePendingTransactions(userWalletId = any(), network = any())
            }
        }
    }

    @Nested
    inner class GetNetworkAddresses {

        @Test
        fun `cardCryptoCurrencyFactory create throws exception`() = runTest {
            // Arrange
            coEvery {
                cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network)
            } throws IllegalStateException()

            // Act
            val actual = repository.getNetworkAddresses(userWalletId = userWalletId, network = network)

            // Assert
            val expected = emptyList<CryptoCurrencyAddress>()

            Truth.assertThat(actual).isEqualTo(expected)

            coVerify { cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network) }
            coVerify(inverse = true) {
                walletManagersFacade.getDefaultAddress(userWalletId = userWalletId, network = network)
            }
        }

        @Test
        fun `cardCryptoCurrencyFactory create returns empty list`() = runTest {
            // Arrange
            coEvery {
                cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network)
            } returns emptyList()

            // Act
            val actual = repository.getNetworkAddresses(userWalletId = userWalletId, network = network)

            // Assert
            val expected = emptyList<CryptoCurrencyAddress>()

            Truth.assertThat(actual).isEqualTo(expected)

            coVerify { cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network) }
            coVerify(inverse = true) {
                walletManagersFacade.getDefaultAddress(userWalletId = userWalletId, network = network)
            }
        }

        @Test
        fun `cardCryptoCurrencyFactory create returns not empty list`() = runTest {
            // Arrange
            val currencies = listOf(cryptoCurrency)

            coEvery {
                cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = network)
            } returns currencies

            coEvery {
                walletManagersFacade.getDefaultAddress(userWalletId = userWalletId, network = cryptoCurrency.network)
            } returns "address"

            // Act
            val actual = repository.getNetworkAddresses(userWalletId = userWalletId, network = network)

            // Assert
            val expected = listOf(
                CryptoCurrencyAddress(cryptoCurrency = cryptoCurrency, address = "address"),
            )

            Truth.assertThat(actual).isEqualTo(expected)

            coVerify {
                cardCryptoCurrencyFactory.create(userWalletId = userWalletId, network = cryptoCurrency.network)
                walletManagersFacade.getDefaultAddress(userWalletId = userWalletId, network = cryptoCurrency.network)
            }
        }
    }

    private companion object {

        val userWalletId = UserWalletId("011")
        val cryptoCurrency = MockCryptoCurrencyFactory().ethereum
        val network = MockCryptoCurrencyFactory().ethereum.network

        val updateWalletManagerResultFactory = MockUpdateWalletManagerResultFactory()

        fun UpdateWalletManagerResult.toNetworkStatus(): NetworkStatus {
            return NetworkStatusFactory.create(
                network = network,
                updatingResult = this,
                addedCurrencies = setOf(cryptoCurrency),
            )
        }
    }
}