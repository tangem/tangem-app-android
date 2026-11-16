package com.tangem.features.earn.impl.overview

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.getEmittedValues
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnAvailabilityTest {

    private val toggles: EarnFeatureToggles = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val walletsFlow = MutableStateFlow<List<UserWallet>?>(value = null)

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val otherWalletId = UserWalletId("fedcba9876543210")

    private val availability = EarnAvailability(
        toggles = toggles,
        userWalletsListRepository = userWalletsListRepository,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(toggles, userWalletsListRepository)
        every { toggles.isEarnEnabled } returns true
        every { userWalletsListRepository.userWallets } returns walletsFlow
        walletsFlow.value = null
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsEarnAvailableByCurrency {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN currency WHEN isEarnAvailable THEN true only for ETH mainnet coin`(model: CurrencyModel) {
            // Arrange
            every { toggles.isEarnEnabled } returns model.isToggleEnabled
            walletsFlow.value = listOf(createHotWallet())

            // Act
            val actual = availability.isEarnAvailable(USER_WALLET_ID, model.currency)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CurrencyModel(
                description = "GIVEN ETH mainnet coin and toggle on WHEN isEarnAvailable THEN true",
                currency = currencyFactory.createCoin(Blockchain.Ethereum),
                isToggleEnabled = true,
                expected = true,
            ),
            CurrencyModel(
                description = "GIVEN ETH testnet coin WHEN isEarnAvailable THEN false",
                currency = createEthereumTestnetCoin(),
                isToggleEnabled = true,
                expected = false,
            ),
            CurrencyModel(
                description = "GIVEN WETH token on Ethereum WHEN isEarnAvailable THEN false",
                currency = currencyFactory.createToken(
                    blockchain = Blockchain.Ethereum,
                    id = "weth",
                    contractAddress = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2",
                    symbol = "WETH",
                    decimals = 18,
                ),
                isToggleEnabled = true,
                expected = false,
            ),
            CurrencyModel(
                description = "GIVEN USDT token on Ethereum WHEN isEarnAvailable THEN false",
                currency = currencyFactory.createToken(
                    blockchain = Blockchain.Ethereum,
                    id = "tether",
                    contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
                    symbol = "USDT",
                    decimals = 6,
                ),
                isToggleEnabled = true,
                expected = false,
            ),
            CurrencyModel(
                description = "GIVEN BSC coin WHEN isEarnAvailable THEN false",
                currency = currencyFactory.createCoin(Blockchain.BSC),
                isToggleEnabled = true,
                expected = false,
            ),
            CurrencyModel(
                description = "GIVEN ETH mainnet coin and toggle off WHEN isEarnAvailable THEN false",
                currency = currencyFactory.createCoin(Blockchain.Ethereum),
                isToggleEnabled = false,
                expected = false,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AvailabilityByWallet {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN wallet WHEN availability checked THEN true only for a suitable wallet`(model: WalletModel) = runTest {
            // Arrange
            walletsFlow.value = model.wallets
            val ethereum = currencyFactory.createCoin(Blockchain.Ethereum)

            // Act
            val actual = Availability(
                isWalletAvailable = availability.isWalletAvailable(USER_WALLET_ID).first(),
                isEarnAvailable = availability.isEarnAvailable(USER_WALLET_ID, ethereum),
            )

            // Assert
            val expected = Availability(isWalletAvailable = model.expected, isEarnAvailable = model.expected)
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            WalletModel(
                description = "GIVEN unlocked hot wallet in list WHEN checked THEN available",
                wallets = listOf(createHotWallet()),
                expected = true,
            ),
            WalletModel(
                description = "GIVEN multi-currency cold wallet in list WHEN checked THEN available",
                wallets = listOf(createColdWallet()),
                expected = true,
            ),
            WalletModel(
                description = "GIVEN single-currency cold wallet in list WHEN checked THEN not available",
                wallets = listOf(createSingleCurrencyColdWallet()),
                expected = false,
            ),
            WalletModel(
                description = "GIVEN locked hot wallet in list WHEN checked THEN not available",
                wallets = listOf(createHotWallet(isLocked = true)),
                expected = false,
            ),
            WalletModel(
                description = "GIVEN locked cold wallet in list WHEN checked THEN not available",
                wallets = listOf(createColdWallet(isLocked = true)),
                expected = false,
            ),
            WalletModel(
                description = "GIVEN wallet id outside the list WHEN checked THEN not available",
                wallets = listOf(createHotWallet(walletId = otherWalletId)),
                expected = false,
            ),
            WalletModel(
                description = "GIVEN list not loaded WHEN checked THEN not available",
                wallets = null,
                expected = false,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsWalletAvailableUpdates {

        @Test
        fun `GIVEN hot wallet unlocked WHEN wallet locked then unlocked THEN emits true false true`() = runTest {
            // Arrange
            walletsFlow.value = listOf(createHotWallet())
            val emitted = getEmittedValues(availability.isWalletAvailable(USER_WALLET_ID))

            // Act
            walletsFlow.value = listOf(createHotWallet(isLocked = true))
            walletsFlow.value = listOf(createHotWallet())

            // Assert
            assertThat(emitted).containsExactly(true, false, true).inOrder()
        }

        @Test
        fun `GIVEN wallet available WHEN wallet removed from list THEN emits true then false`() = runTest {
            // Arrange
            walletsFlow.value = listOf(createHotWallet())
            val emitted = getEmittedValues(availability.isWalletAvailable(USER_WALLET_ID))

            // Act
            walletsFlow.value = listOf(createHotWallet(walletId = otherWalletId))

            // Assert
            assertThat(emitted).containsExactly(true, false).inOrder()
        }

        @Test
        fun `GIVEN wallet available WHEN wallet renamed THEN no new value emitted`() = runTest {
            // Arrange
            val wallet = createHotWallet()
            walletsFlow.value = listOf(wallet)
            val emitted = getEmittedValues(availability.isWalletAvailable(USER_WALLET_ID))

            // Act
            walletsFlow.value = listOf(wallet.copy(name = "Renamed"))

            // Assert
            assertThat(emitted).containsExactly(true)
        }

        @Test
        fun `GIVEN wallet available WHEN wallets reordered THEN no new value emitted`() = runTest {
            // Arrange
            val wallet = createHotWallet()
            val otherWallet = createHotWallet(walletId = otherWalletId)
            walletsFlow.value = listOf(wallet, otherWallet)
            val emitted = getEmittedValues(availability.isWalletAvailable(USER_WALLET_ID))

            // Act
            walletsFlow.value = listOf(otherWallet, wallet)

            // Assert
            assertThat(emitted).containsExactly(true)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EarnToggle {

        @Test
        fun `GIVEN toggle off and suitable wallet WHEN checked THEN wallet available but earn not available`() =
            runTest {
                // Arrange
                every { toggles.isEarnEnabled } returns false
                walletsFlow.value = listOf(createHotWallet())
                val ethereum = currencyFactory.createCoin(Blockchain.Ethereum)

                // Act
                val actual = Availability(
                    isWalletAvailable = availability.isWalletAvailable(USER_WALLET_ID).first(),
                    isEarnAvailable = availability.isEarnAvailable(USER_WALLET_ID, ethereum),
                )

                // Assert
                assertThat(actual).isEqualTo(Availability(isWalletAvailable = true, isEarnAvailable = false))
            }
    }

    private fun createEthereumTestnetCoin(): CryptoCurrency.Coin {
        val coin = currencyFactory.createCoin(Blockchain.EthereumTestnet)
        check(BlockchainUtils.isEthereum(coin.network.rawId) && coin.network.isTestnet) {
            "The testnet row must be rejected by the testnet check, not by the Ethereum id check"
        }
        return coin
    }

    internal data class CurrencyModel(
        val description: String,
        val currency: CryptoCurrency,
        val isToggleEnabled: Boolean,
        val expected: Boolean,
    ) {
        override fun toString(): String = description
    }

    internal data class WalletModel(
        val description: String,
        val wallets: List<UserWallet>?,
        val expected: Boolean,
    ) {
        override fun toString(): String = description
    }

    internal data class Availability(
        val isWalletAvailable: Boolean,
        val isEarnAvailable: Boolean,
    )
}