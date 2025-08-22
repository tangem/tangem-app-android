package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.walletmanager.WalletManagersFacade
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StakingIdFactoryTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val factory = StakingIdFactory(walletManagersFacade = walletManagersFacade)

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManagersFacade)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Create {

        private val defaultAddress = "address"

        @Test
        fun `create returns UnsupportedCurrency if integrationId is null`() = runTest {
            // Arrange
            val userWalletId = UserWalletId(stringValue = "011")
            val currency = MockCryptoCurrencyFactory().ethereum

            // Act
            val actual = factory.create(
                userWalletId = userWalletId,
                currencyId = currency.id,
                network = currency.network,
            )

            // Assert
            val expected = StakingIdFactory.Error.UnsupportedCurrency

            Truth.assertThat(actual.leftOrNull()).isEqualTo(expected)

            coVerify(inverse = true) {
                walletManagersFacade.getDefaultAddress(userWalletId = any(), network = any())
            }
        }

        @Test
        fun `create returns UnableToGetAddress if address is null`() = runTest {
            // Arrange
            val userWalletId = UserWalletId(stringValue = "011")
            val currency = MockCryptoCurrencyFactory().createCoin(Blockchain.TON)

            coEvery {
                walletManagersFacade.getDefaultAddress(userWalletId = userWalletId, network = currency.network)
            } returns null

            // Act
            val actual = factory.create(
                userWalletId = userWalletId,
                currencyId = currency.id,
                network = currency.network,
            )

            // Assert
            val expected = StakingIdFactory.Error.UnableToGetAddress(
                integrationId = StakingIntegrationID.Coin.Ton,
            )

            Truth.assertThat(actual.leftOrNull()).isEqualTo(expected)

            coVerify(exactly = 1) {
                walletManagersFacade.getDefaultAddress(userWalletId = userWalletId, network = currency.network)
            }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun create(model: CreateModel) = runTest {
            // Arrange
            val userWalletId = UserWalletId(stringValue = "011")
            val network = MockCryptoCurrencyFactory().createCoin(Blockchain.TON).network

            coEvery {
                walletManagersFacade.getDefaultAddress(userWalletId = userWalletId, network = network)
            } returns defaultAddress

            // Act
            val actual = factory.create(userWalletId = userWalletId, currencyId = model.currencyId, network = network)

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)

            if (model.expected.leftOrNull() != StakingIdFactory.Error.UnsupportedCurrency) {
                coVerify(exactly = 1) {
                    walletManagersFacade.getDefaultAddress(userWalletId = userWalletId, network = network)
                }
            }
        }

        private fun provideTestModels() = listOf(
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Bitcoin),
                expected = StakingIdFactory.Error.UnsupportedCurrency.left(),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.TON),
                expected = createStakingId(integrationId = StakingIntegrationID.Coin.Ton),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Solana),
                expected = createStakingId(integrationId = StakingIntegrationID.Coin.Solana),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Cosmos),
                expected = createStakingId(integrationId = StakingIntegrationID.Coin.Cosmos),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Tron),
                expected = createStakingId(integrationId = StakingIntegrationID.Coin.Tron),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.BSC),
                expected = createStakingId(integrationId = StakingIntegrationID.Coin.BSC),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Cardano),
                expected = createStakingId(integrationId = StakingIntegrationID.Coin.Cardano),
            ),
            CreateModel(
                currencyId = CryptoCurrency.ID.fromValue(value = "coin⟨ETH⟩polygon-ecosystem-token⚓"),
                expected = createStakingId(integrationId = StakingIntegrationID.EthereumToken.Polygon),
            ),
        )

        private fun createStakingId(integrationId: StakingIntegrationID): Either<Nothing, StakingID> {
            return StakingID(integrationId = integrationId.value, address = defaultAddress).right()
        }
    }

    data class CreateModel(val currencyId: CryptoCurrency.ID, val expected: Either<StakingIdFactory.Error, StakingID>)

    private fun createCurrencyId(blockchain: Blockchain): CryptoCurrency.ID {
        return CryptoCurrency.ID.fromValue(value = "coin⟨${blockchain.id}⟩")
    }
}