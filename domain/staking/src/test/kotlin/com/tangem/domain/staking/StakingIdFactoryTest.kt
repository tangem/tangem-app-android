package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import io.mockk.*
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
    private val stakingFeatureToggles: StakingFeatureToggles = mockk()
    private val factory = StakingIdFactory(
        walletManagersFacade = walletManagersFacade,
        stakingFeatureToggles = stakingFeatureToggles,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManagersFacade, stakingFeatureToggles)
        every { stakingFeatureToggles.isIntegrationEnabled(any()) } returns true
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Create {

        private val defaultAddress = "address"

        @Test
        fun `GIVEN unsupported currency WHEN create THEN UnsupportedCurrency and address not requested`() = runTest {
            // Arrange
            val userWalletId = UserWalletId(stringValue = "011")
            val currency = MockCryptoCurrencyFactory().createCoin(Blockchain.Bitcoin)

            // Act
            val actual = factory.create(
                userWalletId = userWalletId,
                currencyId = currency.id,
                network = currency.network,
            )

            // Assert
            val expected = StakingIdFactory.Error.UnsupportedCurrency

            Truth.assertThat(actual).isEqualTo(expected.left())

            coVerify(inverse = true) {
                walletManagersFacade.getDefaultAddress(userWalletId = any(), network = any())
            }
        }

        @Test
        fun `GIVEN integration disabled by toggle WHEN create THEN UnsupportedCurrency and address not requested`() = runTest {
            // Arrange
            val userWalletId = UserWalletId(stringValue = "011")
            val currency = MockCryptoCurrencyFactory().createCoin(Blockchain.TON)

            every {
                stakingFeatureToggles.isIntegrationEnabled(StakingIntegrationID.StakeKit.Coin.Ton)
            } returns false

            // Act
            val actual = factory.create(
                userWalletId = userWalletId,
                currencyId = currency.id,
                network = currency.network,
            )

            // Assert
            val expected = StakingIdFactory.Error.UnsupportedCurrency

            Truth.assertThat(actual).isEqualTo(expected.left())

            coVerify(inverse = true) {
                walletManagersFacade.getDefaultAddress(userWalletId = any(), network = any())
            }
        }

        @Test
        fun `GIVEN null default address WHEN create THEN UnableToGetAddress`() = runTest {
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
                integrationId = StakingIntegrationID.StakeKit.Coin.Ton,
            )

            Truth.assertThat(actual).isEqualTo(expected.left())

            coVerify(exactly = 1) {
                walletManagersFacade.getDefaultAddress(userWalletId = userWalletId, network = currency.network)
            }
        }

        @Test
        fun `GIVEN empty default address WHEN create THEN UnableToGetAddress`() = runTest {
            // Arrange
            val userWalletId = UserWalletId(stringValue = "011")
            val currency = MockCryptoCurrencyFactory().createCoin(Blockchain.TON)

            coEvery {
                walletManagersFacade.getDefaultAddress(userWalletId = userWalletId, network = currency.network)
            } returns ""

            // Act
            val actual = factory.create(
                userWalletId = userWalletId,
                currencyId = currency.id,
                network = currency.network,
            )

            // Assert
            val expected = StakingIdFactory.Error.UnableToGetAddress(
                integrationId = StakingIntegrationID.StakeKit.Coin.Ton,
            )

            Truth.assertThat(actual).isEqualTo(expected.left())
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN currency id WHEN create THEN expected staking id or error`(model: CreateModel) = runTest {
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
                expected = createStakingId(integrationId = StakingIntegrationID.StakeKit.Coin.Ton),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Solana),
                expected = createStakingId(integrationId = StakingIntegrationID.StakeKit.Coin.Solana),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Cosmos),
                expected = createStakingId(integrationId = StakingIntegrationID.StakeKit.Coin.Cosmos),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Tron),
                expected = createStakingId(integrationId = StakingIntegrationID.StakeKit.Coin.Tron),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.BSC),
                expected = createStakingId(integrationId = StakingIntegrationID.StakeKit.Coin.BSC),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Cardano),
                expected = createStakingId(integrationId = StakingIntegrationID.StakeKit.Coin.Cardano),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = StakingIntegrationID.P2PEthPool.blockchain),
                expected = createStakingId(integrationId = StakingIntegrationID.P2PEthPool),
            ),
            CreateModel(
                currencyId = CryptoCurrency.ID.fromValue(
                    value = "token⟨ethereum⟩polygon-ecosystem-token⚓0x7d1afa7b718fb893db30a3abc0cfc608aacfebb0",
                ),
                expected = createStakingId(integrationId = StakingIntegrationID.StakeKit.EthereumToken.Polygon),
            ),
        )

        private fun createStakingId(integrationId: StakingIntegrationID): Either<Nothing, StakingID> {
            return StakingID(integrationId = integrationId.value, address = defaultAddress).right()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateByCurrency {

        private val userWalletId = UserWalletId(stringValue = "011")

        @Test
        fun `GIVEN solana currency WHEN create THEN address requested for currency network`() = runTest {
            // Arrange
            val solana = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana)
            coEvery { walletManagersFacade.getDefaultAddress(userWalletId, solana.network) } returns "address"

            // Act
            val actual = factory.create(userWalletId = userWalletId, cryptoCurrency = solana)

            // Assert
            Truth.assertThat(actual).isEqualTo(StakingID(MockStaking.SOLANA_ID, "address").right())
            coVerify(exactly = 1) { walletManagersFacade.getDefaultAddress(userWalletId, solana.network) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateSync {

        private val tonId = MockCryptoCurrencyFactory().createCoin(Blockchain.TON).id

        @Test
        fun `GIVEN supported id and address WHEN create sync THEN staking id returned`() {
            // Act
            val actual = factory.create(currencyId = tonId, defaultAddress = "address")

            // Assert
            Truth.assertThat(actual).isEqualTo(StakingID(MockStaking.TON_ID, "address").right())
            coVerify(exactly = 0) { walletManagersFacade.getDefaultAddress(any(), any()) }
        }

        // Pins current behaviour: only null or empty addresses are rejected, blank passes
        @Test
        fun `GIVEN blank address WHEN create sync THEN staking id with blank address`() {
            // Act
            val actual = factory.create(currencyId = tonId, defaultAddress = "  ")

            // Assert
            Truth.assertThat(actual).isEqualTo(StakingID(MockStaking.TON_ID, "  ").right())
        }
    }

    data class CreateModel(val currencyId: CryptoCurrency.ID, val expected: Either<StakingIdFactory.Error, StakingID>)

    private fun createCurrencyId(blockchain: Blockchain): CryptoCurrency.ID {
        return CryptoCurrency.ID.fromValue(value = "coin⟨${blockchain.toNetworkId()}⟩")
    }
}