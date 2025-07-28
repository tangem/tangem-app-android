package com.tangem.data.staking.utils

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingID
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
    inner class CreateIntegrationId {

        @ParameterizedTest
        @ProvideTestModels
        fun createIntegrationId(model: CreateIntegrationIdModel) {
            // Act
            val actual = factory.createIntegrationId(currencyId = model.currencyId)

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CreateIntegrationIdModel(
                currencyId = createCurrencyId(blockchain = Blockchain.TON),
                expected = "ton-ton-chorus-one-pools-staking",
            ),
            CreateIntegrationIdModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Solana),
                expected = "solana-sol-native-multivalidator-staking",
            ),
            CreateIntegrationIdModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Cosmos),
                expected = "cosmos-atom-native-staking",
            ),
            CreateIntegrationIdModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Tron),
                expected = "tron-trx-native-staking",
            ),
            CreateIntegrationIdModel(
                currencyId = CryptoCurrency.ID.fromValue(value = "coin⟨ETH⟩polygon-ecosystem-token⚓"),
                expected = "ethereum-matic-native-staking",
            ),
            CreateIntegrationIdModel(
                currencyId = createCurrencyId(blockchain = Blockchain.BSC),
                expected = "bsc-bnb-native-staking",
            ),
            CreateIntegrationIdModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Cardano),
                expected = "cardano-ada-native-staking",
            ),
            CreateIntegrationIdModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Bitcoin),
                expected = null,
            ),
        )
    }

    data class CreateIntegrationIdModel(val currencyId: CryptoCurrency.ID, val expected: String?)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Create {

        private val defaultAddress = "address"

        @Test
        fun `create returns null if address is null`() = runTest {
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
            val expected = null
            Truth.assertThat(actual).isEqualTo(expected)

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

            coVerify(exactly = 1) {
                walletManagersFacade.getDefaultAddress(userWalletId = userWalletId, network = network)
            }
        }

        private fun provideTestModels() = listOf(
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.TON),
                expected = createStakingId(integrationId = "ton-ton-chorus-one-pools-staking"),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Solana),
                expected = createStakingId(integrationId = "solana-sol-native-multivalidator-staking"),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Cosmos),
                expected = createStakingId(integrationId = "cosmos-atom-native-staking"),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Tron),
                expected = createStakingId(integrationId = "tron-trx-native-staking"),
            ),
            CreateModel(
                currencyId = CryptoCurrency.ID.fromValue(value = "coin⟨ETH⟩polygon-ecosystem-token⚓"),
                expected = createStakingId(integrationId = "ethereum-matic-native-staking"),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.BSC),
                expected = createStakingId(integrationId = "bsc-bnb-native-staking"),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Cardano),
                expected = createStakingId(integrationId = "cardano-ada-native-staking"),
            ),
            CreateModel(
                currencyId = createCurrencyId(blockchain = Blockchain.Bitcoin),
                expected = null,
            ),
        )

        private fun createStakingId(integrationId: String): StakingID {
            return StakingID(integrationId = integrationId, address = defaultAddress)
        }
    }

    data class CreateModel(val currencyId: CryptoCurrency.ID, val expected: StakingID?)

    private fun createCurrencyId(blockchain: Blockchain): CryptoCurrency.ID {
        return CryptoCurrency.ID.fromValue(value = "coin⟨${blockchain.id}⟩${blockchain.toCoinId()}⚓")
    }
}