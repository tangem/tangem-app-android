package com.tangem.data.staking

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.staking.store.StakeKitBalancesStore
import com.tangem.domain.card.configs.CardConfig
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.card.configs.Wallet2CardConfig
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.staking.repositories.StakeKitRepository
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultStakingRepositoryTest {

    private val stakeKitRepository: StakeKitRepository = mockk()
    private val p2pEthPoolRepository: P2PEthPoolRepository = mockk()
    private val stakeKitBalancesStore: StakeKitBalancesStore = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val stakingFeatureToggles: StakingFeatureToggles = mockk()

    private val repository = DefaultStakingRepository(
        stakeKitRepository = stakeKitRepository,
        p2pEthPoolRepository = p2pEthPoolRepository,
        stakeKitBalancesStore = stakeKitBalancesStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
        getUserWalletUseCase = getUserWalletUseCase,
        stakingFeatureToggles = stakingFeatureToggles,
    )

    private val factory = MockCryptoCurrencyFactory()
    private val ethereum = factory.ethereum
    private val jointEthereum = ethereum.onJointPath()
    private val solana = factory.createCoin(Blockchain.Solana)
    private val cosmos = factory.createCoin(Blockchain.Cosmos)
    private val cardano = factory.createCoin(Blockchain.Cardano)
    private val tron = factory.createCoin(Blockchain.Tron)
    private val bitcoin = factory.createCoin(Blockchain.Bitcoin)
    private val polygonToken = factory.createToken(
        blockchain = Blockchain.Ethereum,
        id = "polygon-ecosystem-token",
        contractAddress = MockStaking.POLYGON_CONTRACT,
        symbol = "POL",
        decimals = 18,
    )
    private val nonStakingErc20 = factory.createToken(blockchain = Blockchain.Ethereum)
    private val solanaWithoutRawId = solana.copy(
        id = CryptoCurrency.ID(
            CryptoCurrency.ID.Prefix.COIN_PREFIX,
            CryptoCurrency.ID.Body.NetworkId("solana"),
            CryptoCurrency.ID.Suffix.ContractAddress(""),
        ),
    )

    private val hotWallet = UserWallet.Hot(
        name = "Hot",
        walletId = WALLET_ID,
        hotWalletId = HotWalletId(value = "hot", authType = HotWalletId.AuthType.NoPassword),
        wallets = null,
        isBackedUp = false,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            stakeKitRepository,
            p2pEthPoolRepository,
            stakeKitBalancesStore,
            getUserWalletUseCase,
            stakingFeatureToggles,
        )
    }

    private fun coldWallet(
        batchId: String = "AF04",
        cardConfig: CardConfig = GenericCardConfig(2),
        productType: ProductType = ProductType.Wallet2,
    ): UserWallet.Cold {
        val base = MockScanResponseFactory.create(cardConfig = cardConfig, derivedKeys = emptyMap())
        return MockUserWalletFactory.create(
            base.copy(card = base.card.copy(batchId = batchId), productType = productType),
        )
    }

    private fun stubEnabledIntegrationsAndWallet(
        wallet: UserWallet = hotWallet,
        integration: StakingIntegrationID.StakeKit = StakingIntegrationID.StakeKit.Coin.Solana,
    ) {
        every { stakingFeatureToggles.isIntegrationEnabled(any()) } returns true
        every { getUserWalletUseCase(WALLET_ID) } returns wallet.right()
        stubProviders(integration)
    }

    private fun stubProviders(integration: StakingIntegrationID.StakeKit) {
        coEvery { stakeKitRepository.getStakingAvailabilitySync(integration, any(), any()) } returns SK_AVAILABLE
        every { stakeKitRepository.getStakingAvailability(integration, any(), any()) } returns flowOf(SK_AVAILABLE)
        coEvery { p2pEthPoolRepository.getStakingAvailabilitySync() } returns P2P_AVAILABLE
        every { p2pEthPoolRepository.getStakingAvailability() } returns flowOf(P2P_AVAILABLE)
    }

    private fun verifyStakeKitNotCalled() {
        verify { stakeKitRepository wasNot Called }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class JointAccount {

        @Test
        fun `GIVEN personal coin WHEN getStakingAvailabilitySync THEN integration repository answers`() = runTest {
            // Arrange
            every { stakingFeatureToggles.isIntegrationEnabled(any()) } returns true
            every { getUserWalletUseCase(WALLET_ID) } returns hotWallet.right()
            coEvery { p2pEthPoolRepository.getStakingAvailabilitySync() } returns P2P_AVAILABLE

            // Act
            val actual = repository.getStakingAvailabilitySync(WALLET_ID, ethereum)

            // Assert
            assertThat(actual).isEqualTo(P2P_AVAILABLE)
        }

        @Test
        fun `GIVEN joint account coin WHEN getStakingAvailabilitySync THEN unavailable without asking integrations`() =
            runTest {
                // Act
                val actual = repository.getStakingAvailabilitySync(WALLET_ID, jointEthereum)

                // Assert
                assertThat(actual).isEqualTo(StakingAvailability.Unavailable)
                coVerify(exactly = 0) { p2pEthPoolRepository.getStakingAvailabilitySync() }
            }

        @Test
        fun `GIVEN joint account coin WHEN getStakingAvailability THEN unavailable without asking integrations`() =
            runTest {
                // Act
                val actual = repository.getStakingAvailability(WALLET_ID, jointEthereum).first()

                // Assert
                assertThat(actual).isEqualTo(StakingAvailability.Unavailable)
                coVerify(exactly = 0) { p2pEthPoolRepository.getStakingAvailability() }
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Routing {

        @ParameterizedTest
        @MethodSource("provideRoutes")
        fun `GIVEN supported currency WHEN getStakingAvailabilitySync THEN provider repository of its integration answers`(
            model: RouteModel,
        ) = runTest {
            // Arrange
            stubEnabledIntegrationsAndWallet(integration = model.stakeKitIntegration)

            // Act
            val actual = repository.getStakingAvailabilitySync(WALLET_ID, model.currency)

            // Assert
            if (model.integration is StakingIntegrationID.StakeKit) {
                assertThat(actual).isEqualTo(SK_AVAILABLE)
                coVerify {
                    stakeKitRepository.getStakingAvailabilitySync(
                        model.integration,
                        model.currency.id.rawCurrencyId!!,
                        model.currency.symbol,
                    )
                }
                coVerify(exactly = 0) { p2pEthPoolRepository.getStakingAvailabilitySync() }
            } else {
                assertThat(actual).isEqualTo(P2P_AVAILABLE)
                verifyStakeKitNotCalled()
            }
        }

        @ParameterizedTest
        @MethodSource("provideRoutes")
        fun `GIVEN supported currency WHEN getStakingAvailability THEN provider flow forwarded`(model: RouteModel) =
            runTest {
                // Arrange
                stubEnabledIntegrationsAndWallet(integration = model.stakeKitIntegration)

                // Act
                val actual = repository.getStakingAvailability(WALLET_ID, model.currency).first()

                // Assert
                if (model.integration is StakingIntegrationID.StakeKit) {
                    assertThat(actual).isEqualTo(SK_AVAILABLE)
                    verify {
                        stakeKitRepository.getStakingAvailability(
                            model.integration,
                            model.currency.id.rawCurrencyId!!,
                            model.currency.symbol,
                        )
                    }
                    verify(exactly = 0) { p2pEthPoolRepository.getStakingAvailability() }
                } else {
                    assertThat(actual).isEqualTo(P2P_AVAILABLE)
                    verifyStakeKitNotCalled()
                }
            }

        @Test
        fun `GIVEN provider emits several values WHEN getStakingAvailability THEN all forwarded in order`() = runTest {
            // Arrange
            stubEnabledIntegrationsAndWallet()
            every {
                stakeKitRepository.getStakingAvailability(StakingIntegrationID.StakeKit.Coin.Solana, any(), any())
            } returns flowOf(StakingAvailability.TemporaryUnavailable, SK_AVAILABLE)

            // Act
            val actual = repository.getStakingAvailability(WALLET_ID, solana).toList()

            // Assert
            assertThat(actual).containsExactly(StakingAvailability.TemporaryUnavailable, SK_AVAILABLE).inOrder()
        }

        private fun provideRoutes() = listOf(
            RouteModel("Ton", StakingIntegrationID.StakeKit.Coin.Ton, factory.createCoin(Blockchain.TON)),
            RouteModel("Solana", StakingIntegrationID.StakeKit.Coin.Solana, solana),
            RouteModel("Cosmos", StakingIntegrationID.StakeKit.Coin.Cosmos, cosmos),
            RouteModel("Tron", StakingIntegrationID.StakeKit.Coin.Tron, tron),
            RouteModel("BSC", StakingIntegrationID.StakeKit.Coin.BSC, factory.createCoin(Blockchain.BSC)),
            RouteModel("Cardano", StakingIntegrationID.StakeKit.Coin.Cardano, cardano),
            RouteModel("Polygon", StakingIntegrationID.StakeKit.EthereumToken.Polygon, polygonToken),
            RouteModel("P2PEthPool", StakingIntegrationID.P2PEthPool, ethereum),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Unavailable {

        @Test
        fun `GIVEN unsupported coin WHEN getStakingAvailabilitySync THEN unavailable without toggle check`() = runTest {
            // Act
            val actual = repository.getStakingAvailabilitySync(WALLET_ID, bitcoin)

            // Assert
            assertThat(actual).isEqualTo(StakingAvailability.Unavailable)
            verify(exactly = 0) { stakingFeatureToggles.isIntegrationEnabled(any()) }
        }

        @Test
        fun `GIVEN unsupported coin WHEN getStakingAvailability THEN unavailable`() = runTest {
            // Act
            val actual = repository.getStakingAvailability(WALLET_ID, bitcoin).first()

            // Assert
            assertThat(actual).isEqualTo(StakingAvailability.Unavailable)
        }

        @Test
        fun `GIVEN non polygon erc20 token WHEN getStakingAvailabilitySync THEN unavailable`() = runTest {
            // Arrange
            stubEnabledIntegrationsAndWallet()

            // Act
            val actual = repository.getStakingAvailabilitySync(WALLET_ID, nonStakingErc20)

            // Assert
            assertThat(actual).isEqualTo(StakingAvailability.Unavailable)
        }

        @Test
        fun `GIVEN integration toggle disabled WHEN getStakingAvailabilitySync THEN unavailable and wallet not loaded`() =
            runTest {
                // Arrange
                every { stakingFeatureToggles.isIntegrationEnabled(StakingIntegrationID.StakeKit.Coin.Solana) } returns
                    false

                // Act
                val actual = repository.getStakingAvailabilitySync(WALLET_ID, solana)

                // Assert
                assertThat(actual).isEqualTo(StakingAvailability.Unavailable)
                verify(exactly = 0) { getUserWalletUseCase(any()) }
            }

        @Test
        fun `GIVEN integration toggle disabled WHEN getStakingAvailability THEN unavailable`() = runTest {
            // Arrange
            every { stakingFeatureToggles.isIntegrationEnabled(StakingIntegrationID.StakeKit.Coin.Solana) } returns
                false

            // Act
            val actual = repository.getStakingAvailability(WALLET_ID, solana).first()

            // Assert
            assertThat(actual).isEqualTo(StakingAvailability.Unavailable)
        }

        @Test
        fun `GIVEN wallet not found WHEN getStakingAvailabilitySync THEN unavailable`() = runTest {
            // Arrange
            every { stakingFeatureToggles.isIntegrationEnabled(any()) } returns true
            every { getUserWalletUseCase(WALLET_ID) } returns GetUserWalletError.UserWalletNotFound.left()

            // Act
            val actual = repository.getStakingAvailabilitySync(WALLET_ID, solana)

            // Assert
            assertThat(actual).isEqualTo(StakingAvailability.Unavailable)
            verifyStakeKitNotCalled()
        }

        @Test
        fun `GIVEN wallet not found WHEN getStakingAvailability THEN unavailable`() = runTest {
            // Arrange
            every { stakingFeatureToggles.isIntegrationEnabled(any()) } returns true
            every { getUserWalletUseCase(WALLET_ID) } returns GetUserWalletError.UserWalletNotFound.left()

            // Act
            val actual = repository.getStakingAvailability(WALLET_ID, solana).first()

            // Assert
            assertThat(actual).isEqualTo(StakingAvailability.Unavailable)
        }

        @Test
        fun `GIVEN coin without raw currency id WHEN getStakingAvailabilitySync THEN unavailable`() = runTest {
            // Arrange
            stubEnabledIntegrationsAndWallet()

            // Act
            val actual = repository.getStakingAvailabilitySync(WALLET_ID, solanaWithoutRawId)

            // Assert
            assertThat(actual).isEqualTo(StakingAvailability.Unavailable)
            verifyStakeKitNotCalled()
        }

        @Test
        fun `GIVEN coin without raw currency id WHEN getStakingAvailability THEN unavailable`() = runTest {
            // Arrange
            stubEnabledIntegrationsAndWallet()

            // Act
            val actual = repository.getStakingAvailability(WALLET_ID, solanaWithoutRawId).first()

            // Assert
            assertThat(actual).isEqualTo(StakingAvailability.Unavailable)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CardRules {

        @ParameterizedTest
        @MethodSource("provideBlockedBatches")
        fun `GIVEN solana and blocked card batch WHEN getStakingAvailabilitySync THEN unavailable`(batch: String) =
            runTest {
                // Arrange
                stubEnabledIntegrationsAndWallet(coldWallet(batchId = batch))

                // Act
                val actual = repository.getStakingAvailabilitySync(WALLET_ID, solana)

                // Assert
                assertThat(actual).isEqualTo(StakingAvailability.Unavailable)
                verifyStakeKitNotCalled()
            }

        @Test
        fun `GIVEN solana and allowed card batch WHEN getStakingAvailabilitySync THEN stakeKit answers`() = runTest {
            // Arrange
            stubEnabledIntegrationsAndWallet(coldWallet(batchId = "AF04"))

            // Act
            val actual = repository.getStakingAvailabilitySync(WALLET_ID, solana)

            // Assert
            assertThat(actual).isEqualTo(SK_AVAILABLE)
        }

        @Test
        fun `GIVEN cosmos and blocked solana batch WHEN getStakingAvailabilitySync THEN stakeKit answers`() = runTest {
            // Arrange
            stubEnabledIntegrationsAndWallet(coldWallet(batchId = "AC01"), StakingIntegrationID.StakeKit.Coin.Cosmos)

            // Act
            val actual = repository.getStakingAvailabilitySync(WALLET_ID, cosmos)

            // Assert
            assertThat(actual).isEqualTo(SK_AVAILABLE)
        }

        @Test
        fun `GIVEN cardano and card without wallet2 firmware WHEN getStakingAvailabilitySync THEN unavailable`() =
            runTest {
                // Arrange
                stubEnabledIntegrationsAndWallet(coldWallet(cardConfig = GenericCardConfig(2)))

                // Act
                val actual = repository.getStakingAvailabilitySync(WALLET_ID, cardano)

                // Assert
                assertThat(actual).isEqualTo(StakingAvailability.Unavailable)
            }

        @Test
        fun `GIVEN cardano and wallet2 card WHEN getStakingAvailabilitySync THEN stakeKit answers`() = runTest {
            // Arrange
            stubEnabledIntegrationsAndWallet(
                coldWallet(cardConfig = Wallet2CardConfig),
                StakingIntegrationID.StakeKit.Coin.Cardano,
            )

            // Act
            val actual = repository.getStakingAvailabilitySync(WALLET_ID, cardano)

            // Assert
            assertThat(actual).isEqualTo(SK_AVAILABLE)
        }

        @ParameterizedTest
        @MethodSource("provideNoteCurrencies")
        fun `GIVEN note card WHEN getStakingAvailabilitySync THEN unavailable for any currency`(
            model: NoteCurrencyModel,
        ) = runTest {
            // Arrange
            stubEnabledIntegrationsAndWallet(coldWallet(productType = ProductType.Note))

            // Act
            val actual = repository.getStakingAvailabilitySync(WALLET_ID, model.currency)

            // Assert
            assertThat(actual).isEqualTo(StakingAvailability.Unavailable)
        }

        @Test
        fun `GIVEN hot wallet and solana WHEN getStakingAvailabilitySync THEN card rules skipped`() = runTest {
            // Arrange
            stubEnabledIntegrationsAndWallet(hotWallet)

            // Act
            val actual = repository.getStakingAvailabilitySync(WALLET_ID, solana)

            // Assert
            assertThat(actual).isEqualTo(SK_AVAILABLE)
        }

        @Test
        fun `GIVEN solana and blocked card batch WHEN getStakingAvailability THEN unavailable`() = runTest {
            // Arrange
            stubEnabledIntegrationsAndWallet(coldWallet(batchId = "AC01"))

            // Act
            val actual = repository.getStakingAvailability(WALLET_ID, solana).first()

            // Assert
            assertThat(actual).isEqualTo(StakingAvailability.Unavailable)
            verifyStakeKitNotCalled()
        }

        private fun provideBlockedBatches() = listOf("AC01", "CB79")

        private fun provideNoteCurrencies() = listOf(
            NoteCurrencyModel("Ethereum", ethereum),
            NoteCurrencyModel("Solana", solana),
            NoteCurrencyModel("Tron", tron),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsAnyTokenStaked {

        private val stakingId = MockStaking.stakingId()

        @Test
        fun `GIVEN no balances stored WHEN isAnyTokenStaked THEN false`() = runTest {
            // Arrange
            coEvery { stakeKitBalancesStore.getAllSyncOrNull(WALLET_ID) } returns null

            // Act
            val actual = repository.isAnyTokenStaked(WALLET_ID)

            // Assert
            assertThat(actual).isFalse()
        }

        @Test
        fun `GIVEN empty balances WHEN isAnyTokenStaked THEN false`() = runTest {
            // Arrange
            coEvery { stakeKitBalancesStore.getAllSyncOrNull(WALLET_ID) } returns emptySet()

            // Act
            val actual = repository.isAnyTokenStaked(WALLET_ID)

            // Assert
            assertThat(actual).isFalse()
        }

        @Test
        fun `GIVEN only empty and error balances WHEN isAnyTokenStaked THEN false`() = runTest {
            // Arrange
            coEvery { stakeKitBalancesStore.getAllSyncOrNull(WALLET_ID) } returns setOf(
                StakingBalance.Empty(stakingId, StatusSource.ACTUAL),
                StakingBalance.Error(MockStaking.stakingId(address = "0x2")),
            )

            // Act
            val actual = repository.isAnyTokenStaked(WALLET_ID)

            // Assert
            assertThat(actual).isFalse()
        }

        @Test
        fun `GIVEN error and data balances WHEN isAnyTokenStaked THEN true`() = runTest {
            // Arrange
            coEvery { stakeKitBalancesStore.getAllSyncOrNull(WALLET_ID) } returns setOf(
                StakingBalance.Error(stakingId),
                MockStaking.stakeKitBalance(address = "0x2"),
            )

            // Act
            val actual = repository.isAnyTokenStaked(WALLET_ID)

            // Assert
            assertThat(actual).isTrue()
        }
    }

    private fun CryptoCurrency.Coin.onJointPath(): CryptoCurrency.Coin {
        val jointPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/0")

        return copy(
            network = network.copy(
                id = network.id.copy(derivationPath = jointPath),
                derivationPath = jointPath,
            ),
        )
    }

    internal data class RouteModel(
        val name: String,
        val integration: StakingIntegrationID,
        val currency: CryptoCurrency,
    ) {
        val stakeKitIntegration: StakingIntegrationID.StakeKit
            get() = integration as? StakingIntegrationID.StakeKit ?: StakingIntegrationID.StakeKit.Coin.Solana

        override fun toString(): String = name
    }

    internal data class NoteCurrencyModel(val name: String, val currency: CryptoCurrency) {
        override fun toString(): String = name
    }

    private companion object {
        val WALLET_ID = UserWalletId("011")

        val SK_AVAILABLE = StakingAvailability.Available(
            StakingOption.StakeKit(StakingIntegrationID.StakeKit.Coin.Solana, MockStaking.solanaYield),
        )
        val P2P_AVAILABLE = StakingAvailability.Available(StakingOption.P2PEthPool(listOf(MockStaking.vault())))
    }
}