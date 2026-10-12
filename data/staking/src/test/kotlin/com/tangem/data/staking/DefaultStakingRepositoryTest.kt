package com.tangem.data.staking

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.staking.store.StakeKitBalancesStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.staking.repositories.StakeKitRepository
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

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

    private val ethereum = MockCryptoCurrencyFactory().ethereum
    private val jointEthereum = ethereum.onJointPath()

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakeKitRepository, p2pEthPoolRepository, getUserWalletUseCase, stakingFeatureToggles)
    }

    @Test
    fun `GIVEN personal coin WHEN getStakingAvailabilitySync THEN integration repository answers`() = runTest {
        // Arrange
        every { stakingFeatureToggles.isIntegrationEnabled(any()) } returns true
        every { getUserWalletUseCase(WALLET_ID) } returns mockk<UserWallet.Hot>().right()
        val available = StakingAvailability.Available(option = mockk())
        coEvery { p2pEthPoolRepository.getStakingAvailabilitySync() } returns available

        // Act
        val actual = repository.getStakingAvailabilitySync(WALLET_ID, ethereum)

        // Assert
        assertThat(actual).isEqualTo(available)
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

    private fun CryptoCurrency.Coin.onJointPath(): CryptoCurrency.Coin {
        val jointPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/0")

        return copy(
            network = network.copy(
                id = network.id.copy(derivationPath = jointPath),
                derivationPath = jointPath,
            ),
        )
    }

    private companion object {
        val WALLET_ID = UserWalletId("011")
    }
}