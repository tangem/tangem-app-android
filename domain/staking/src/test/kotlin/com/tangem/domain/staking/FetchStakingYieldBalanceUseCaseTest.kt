package com.tangem.domain.staking

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.single.SingleStakingBalanceFetcher
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.mock.staking.MockStaking
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FetchStakingYieldBalanceUseCaseTest {

    private val fetcher: SingleStakingBalanceFetcher = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val stakingFeatureToggles: StakingFeatureToggles = mockk()

    private val useCase = FetchStakingYieldBalanceUseCase(
        singleStakingBalanceFetcher = fetcher,
        stakingIdFactory = StakingIdFactory(walletManagersFacade, stakingFeatureToggles),
    )

    private val userWalletId = UserWalletId(stringValue = "011")
    private val solana = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana)

    @BeforeEach
    fun resetMocks() {
        clearMocks(fetcher, walletManagersFacade, stakingFeatureToggles)
        every { stakingFeatureToggles.isIntegrationEnabled(any()) } returns true
        coEvery { walletManagersFacade.getDefaultAddress(userWalletId, solana.network) } returns "address"
        coEvery { fetcher(any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN supported currency WHEN invoke THEN fetcher called with staking id and Right returned`() = runTest {
        // Act
        val actual = useCase(userWalletId, solana)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            fetcher(SingleStakingBalanceFetcher.Params(userWalletId, StakingID(MockStaking.SOLANA_ID, "address")))
        }
    }

    @Test
    fun `GIVEN unsupported currency WHEN invoke THEN Right without fetching`() = runTest {
        // Arrange
        val bitcoin = MockCryptoCurrencyFactory().createCoin(Blockchain.Bitcoin)

        // Act
        val actual = useCase(userWalletId, bitcoin)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 0) { fetcher(any()) }
        coVerify(exactly = 0) { walletManagersFacade.getDefaultAddress(any(), any()) }
    }

    @Test
    fun `GIVEN integration disabled WHEN invoke THEN Right without fetching`() = runTest {
        // Arrange
        every {
            stakingFeatureToggles.isIntegrationEnabled(StakingIntegrationID.StakeKit.Coin.Solana)
        } returns false

        // Act
        val actual = useCase(userWalletId, solana)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 0) { fetcher(any()) }
    }

    @Test
    fun `GIVEN no default address WHEN invoke THEN DomainError with factory error text`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.getDefaultAddress(userWalletId, solana.network) } returns null

        // Act
        val actual = useCase(userWalletId, solana)

        // Assert
        assertThat(actual).isEqualTo(StakingError.DomainError("UnableToGetAddress(integrationId=Solana)").left())
        coVerify(exactly = 0) { fetcher(any()) }
    }

    @Test
    fun `GIVEN fetcher Left WHEN invoke THEN DomainError with throwable text`() = runTest {
        // Arrange
        coEvery { fetcher(any()) } returns IllegalStateException("boom").left()

        // Act
        val actual = useCase(userWalletId, solana)

        // Assert
        assertThat(actual).isEqualTo(StakingError.DomainError("java.lang.IllegalStateException: boom").left())
    }
}