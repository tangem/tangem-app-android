package com.tangem.data.networks.fetcher

import arrow.core.Either
import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult
import com.tangem.common.test.domain.network.MockNetworkStatusFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.walletmanager.MockUpdateWalletManagerResultFactory
import com.tangem.data.networks.store.NetworksStatusesStore
import com.tangem.data.networks.store.setSourceAsOnlyCache
import com.tangem.data.networks.store.storeStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CommonNetworkStatusFetcherTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val networksStatusesStore: NetworksStatusesStore = mockk(relaxUnitFun = true)

    private val fetcher = CommonNetworkStatusFetcher(
        walletManagersFacade = walletManagersFacade,
        networksStatusesStore = networksStatusesStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManagersFacade, networksStatusesStore)
    }

    @Test
    fun `fetch failure if walletManagersFacade throws exception`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("011")
        val network = cryptoCurrencyFactory.ethereum.network
        val extraTokens = setOf(
            cryptoCurrencyFactory.createToken(Blockchain.Ethereum),
        )
        val updateException = IllegalStateException()

        coEvery { walletManagersFacade.update(userWalletId, network, extraTokens) } throws updateException

        // Act
        val actual = fetcher.fetch(userWalletId = userWalletId, network = network, networkCurrencies = extraTokens)

        // Assert
        val expected = Either.Left(updateException)

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected.leftOrNull()!!::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.leftOrNull()!!.message)

        coVerifyOrder {
            walletManagersFacade.update(userWalletId, network, extraTokens)
        }
    }

    @Test
    fun `fetch skips wallet manager update when networkCurrencies is empty`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("011")
        val network = cryptoCurrencyFactory.ethereum.network

        // Act
        val actual = fetcher.fetch(
            userWalletId = userWalletId,
            network = network,
            networkCurrencies = emptySet(),
        )

        // Assert: returns success without touching the wallet manager facade — guard prevents storing
        // a NetworkStatus.Verified with empty `amounts` over an existing valid cached status.
        Truth.assertThat(actual).isEqualTo(Either.Right(Unit))

        coVerify(inverse = true) {
            walletManagersFacade.update(
                userWalletId = any(),
                network = any(),
                extraTokens = any(),
            )
        }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `fetch successfully for any result of walletManagersFacade`(model: SuccessTestModel) = runTest {
        // Arrange
        val userWalletId = UserWalletId("011")
        val network = cryptoCurrencyFactory.ethereum.network
        val extraTokens = setOf(
            cryptoCurrencyFactory.createToken(Blockchain.Ethereum),
        )
        val updateResult = model.updateResult
        val status = model.status

        coEvery { walletManagersFacade.update(userWalletId, network, extraTokens) } returns updateResult
        coEvery { networksStatusesStore.storeStatus(userWalletId = userWalletId, status = status) } returns Unit

        // Act
        val actual = fetcher.fetch(userWalletId = userWalletId, network = network, networkCurrencies = extraTokens)

        // Assert
        val expected = Either.Right(Unit)

        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            walletManagersFacade.update(userWalletId, network, extraTokens)
            networksStatusesStore.storeStatus(userWalletId = userWalletId, status = status)
        }

        coVerify(inverse = true) {
            networksStatusesStore.setSourceAsOnlyCache(userWalletId = any(), network = any())
        }
    }

    @Test
    fun `GIVEN joint network with a Safe manager WHEN fetch THEN the update result is stored`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("011")
        val network = jointNetwork
        val coin = cryptoCurrencyFactory.ethereum.copy(network = network)
        val safeAddress = "0x5afe"
        val updateResult = UpdateWalletManagerResult.Verified(
            selectedAddress = safeAddress,
            addresses = setOf(
                UpdateWalletManagerResult.Address(safeAddress, UpdateWalletManagerResult.Address.Type.Primary),
            ),
            currenciesAmounts = setOf(UpdateWalletManagerResult.CryptoCurrencyAmount.Coin(java.math.BigDecimal.ONE)),
            currentTransactions = emptySet(),
        )
        val expectedStatus = NetworkStatus(
            network = network,
            value = NetworkStatus.Verified(
                address = NetworkAddress.Single(
                    NetworkAddress.Address(safeAddress, NetworkAddress.Address.Type.Primary),
                ),
                amounts = mapOf(coin.id to NetworkStatus.Amount.Loaded(java.math.BigDecimal.ONE)),
                pendingTransactions = emptyMap(),
                yieldSupplyStatuses = mapOf(coin.id to null),
                source = com.tangem.domain.models.StatusSource.ACTUAL,
            ),
        )
        coEvery { walletManagersFacade.getOrCreateWalletManager(userWalletId, network) } returns mockk()
        coEvery { walletManagersFacade.update(userWalletId, network, emptySet()) } returns updateResult
        coEvery { networksStatusesStore.storeStatus(userWalletId = userWalletId, status = expectedStatus) } returns Unit

        // Act
        val actual = fetcher.fetch(userWalletId = userWalletId, network = network, networkCurrencies = setOf(coin))

        // Assert
        Truth.assertThat(actual).isEqualTo(Either.Right(Unit))
        coVerifyOrder {
            walletManagersFacade.update(userWalletId, network, emptySet())
            networksStatusesStore.storeStatus(userWalletId = userWalletId, status = expectedStatus)
        }
    }

    @Test
    fun `GIVEN joint network without a Safe manager yet WHEN fetch THEN nothing is fetched or stored`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("011")
        val coin = cryptoCurrencyFactory.ethereum.copy(network = jointNetwork)
        coEvery { walletManagersFacade.getOrCreateWalletManager(userWalletId, jointNetwork) } returns null

        // Act
        val actual = fetcher.fetch(userWalletId = userWalletId, network = jointNetwork, networkCurrencies = setOf(coin))

        // Assert
        Truth.assertThat(actual).isEqualTo(Either.Right(Unit))
        coVerify(inverse = true) { walletManagersFacade.update(any(), any(), any(), any()) }
        verify { networksStatusesStore wasNot Called }
    }

    @Test
    fun `GIVEN the Safe manager cannot be built WHEN fetch THEN the failure is returned and the cache downgraded`() =
        runTest {
            // Arrange
            val userWalletId = UserWalletId("011")
            val coin = cryptoCurrencyFactory.ethereum.copy(network = jointNetwork)
            val cause = IllegalArgumentException("Unable to create the Safe wallet manager")
            coEvery { walletManagersFacade.getOrCreateWalletManager(userWalletId, jointNetwork) } throws cause
            coEvery {
                networksStatusesStore.setSourceAsOnlyCache(userWalletId = userWalletId, network = jointNetwork)
            } returns Unit

            // Act
            val actual = fetcher.fetch(
                userWalletId = userWalletId,
                network = jointNetwork,
                networkCurrencies = setOf(coin),
            )

            // Assert
            Truth.assertThat(actual).isEqualTo(Either.Left(cause))
            coVerify(inverse = true) { walletManagersFacade.update(any(), any(), any(), any()) }
        }

    private val jointNetwork = cryptoCurrencyFactory.ethereum.network.let { network ->
        val derivationPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/0")

        network.copy(
            id = Network.ID(rawId = network.id.rawId, derivationPath = derivationPath),
            derivationPath = derivationPath,
        )
    }

    @Suppress("unused")
    private fun provideTestModels() = listOf(
        SuccessTestModel(
            updateResult = UpdateWalletManagerResult.MissedDerivation,
            status = MockNetworkStatusFactory.createMissedDerivation(),
        ),
        SuccessTestModel(
            updateResult = MockUpdateWalletManagerResultFactory().createUnreachable(),
            status = NetworkStatus(
                network = cryptoCurrencyFactory.ethereum.network,
                value = NetworkStatus.Unreachable(address = null),
            ),
        ),
        SuccessTestModel(
            updateResult = MockUpdateWalletManagerResultFactory().createUnreachableWithAddress(),
            status = NetworkStatus(
                network = cryptoCurrencyFactory.ethereum.network,
                value = NetworkStatus.Unreachable(
                    address = NetworkAddress.Single(
                        defaultAddress = NetworkAddress.Address(
                            value = "0x1",
                            type = NetworkAddress.Address.Type.Primary,
                        ),
                    ),
                ),
            ),
        ),
        SuccessTestModel(
            updateResult = MockUpdateWalletManagerResultFactory().createNoAccount(),
            status = MockNetworkStatusFactory.createNoAccount(),
        ),
        SuccessTestModel(
            updateResult = MockUpdateWalletManagerResultFactory().createVerified(),
            status = MockNetworkStatusFactory.createVerified(cryptoCurrencyFactory.ethereum.network) {
                it.copy(
                    amounts = mapOf(
                        CryptoCurrency.ID.fromValue(
                            value = "token⟨ethereum⟩NEVER-MIND⚓NEVER-MIND",
                        ) to NetworkStatus.Amount.NotFound,
                    ),
                    pendingTransactions = mapOf(
                        CryptoCurrency.ID.fromValue(value = "token⟨ethereum⟩NEVER-MIND⚓NEVER-MIND") to emptySet(),
                    ),
                    yieldSupplyStatuses = mapOf(
                        CryptoCurrency.ID.fromValue(
                            value = "token⟨ethereum⟩NEVER-MIND⚓NEVER-MIND",
                        ) to null,
                    ),
                )
            },
        ),
    )

    data class SuccessTestModel(
        val updateResult: UpdateWalletManagerResult,
        val status: NetworkStatus,
    )
}