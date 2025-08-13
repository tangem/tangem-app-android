package com.tangem.data.networks.fetcher

import arrow.core.Either
import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult
import com.tangem.common.test.domain.network.MockNetworkStatusFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.walletmanager.MockUpdateWalletManagerResultFactory
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.data.networks.store.NetworksStatusesStore
import com.tangem.data.networks.store.setSourceAsOnlyCache
import com.tangem.data.networks.store.storeStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
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
            cryptoCurrencyFactory.createToken(Blockchain.Ethereum) as CryptoCurrency.Token,
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

    @ParameterizedTest
    @ProvideTestModels
    fun `fetch successfully for any result of walletManagersFacade`(model: SuccessTestModel) = runTest {
        // Arrange
        val userWalletId = UserWalletId("011")
        val network = cryptoCurrencyFactory.ethereum.network
        val extraTokens = setOf(
            cryptoCurrencyFactory.createToken(Blockchain.Ethereum) as CryptoCurrency.Token,
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
                            value = "token⟨ETH⟩NEVER-MIND⚓NEVER-MIND",
                        ) to NetworkStatus.Amount.NotFound,
                    ),
                    pendingTransactions = mapOf(
                        CryptoCurrency.ID.fromValue(value = "token⟨ETH⟩NEVER-MIND⚓NEVER-MIND") to emptySet(),
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