package com.tangem.features.earn.staking.impl

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.features.earn.staking.impl.fixtures.coin
import com.tangem.features.earn.staking.impl.fixtures.currencyStatus
import com.tangem.features.earn.staking.impl.fixtures.polygonToken
import com.tangem.test.mock.staking.MockStaking
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class StakingRateSourceTest {

    private val accountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val stakingRepository: StakingRepository = mockk()

    private val source = StakingRateSource(
        accountStatusListSupplier = accountStatusListSupplier,
        stakingRepository = stakingRepository,
        mapper = StakingRateMapper(),
    )

    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val solana = coin(Blockchain.Solana)
    private val available = StakingAvailability.Available(
        StakingOption.StakeKit(StakingIntegrationID.StakeKit.Coin.Solana, MockStaking.solanaYield),
    )

    @Test
    fun `GIVEN source WHEN reading metadata THEN id staking_stakekit, rank 1 and an id different from the pooled staking product`() {
        // Assert
        assertThat(source.id).isEqualTo(EarnProductId("staking_stakekit"))
        assertThat(source.rank).isEqualTo(1)
        assertThat(source.id).isNotEqualTo(POOLED_STAKING_ID)
    }

    @Test
    fun `GIVEN Solana, a Polygon token, ETH and Bitcoin WHEN rates THEN entries only for Solana and the Polygon token`() =
        runTest {
            // Arrange
            val polygon = polygonToken()
            val ethereum = coin(Blockchain.Ethereum)
            val bitcoin = coin(Blockchain.Bitcoin)
            val polygonAvailability = StakingAvailability.Available(
                StakingOption.StakeKit(StakingIntegrationID.StakeKit.EthereumToken.Polygon, MockStaking.polygonYield),
            )
            every { accountStatusListSupplier(userWalletId) } returns flowOf(
                accountList(
                    currencyStatus(solana),
                    currencyStatus(polygon),
                    currencyStatus(ethereum),
                    currencyStatus(bitcoin),
                ),
            )
            every { stakingRepository.getStakingAvailability(userWalletId, solana) } returns flowOf(available)
            every { stakingRepository.getStakingAvailability(userWalletId, polygon) } returns
                flowOf(polygonAvailability)

            // Act
            val actual = source.rates(userWalletId).first()

            // Assert
            assertThat(actual.keys).containsExactly(solana.id, polygon.id)
            verify(exactly = 0) { stakingRepository.getStakingAvailability(userWalletId, ethereum) }
            verify(exactly = 0) { stakingRepository.getStakingAvailability(userWalletId, bitcoin) }
        }

    @Test
    fun `GIVEN two Solana accounts WHEN rates THEN an entry per currency id`() = runTest {
        // Arrange
        val second = secondAccountOf(solana)
        every { accountStatusListSupplier(userWalletId) } returns
            flowOf(accountList(currencyStatus(solana), currencyStatus(second)))
        every { stakingRepository.getStakingAvailability(userWalletId, solana) } returns flowOf(available)
        every { stakingRepository.getStakingAvailability(userWalletId, second) } returns flowOf(available)

        // Act
        val actual = source.rates(userWalletId).first()

        // Assert
        assertThat(actual.keys).containsExactly(solana.id, second.id)
    }

    @Test
    fun `GIVEN no staking currency WHEN rates THEN empty map`() = runTest {
        // Arrange
        every { accountStatusListSupplier(userWalletId) } returns
            flowOf(accountList(currencyStatus(coin(Blockchain.Bitcoin))))

        // Act
        val actual = source.rates(userWalletId).first()

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN no accounts WHEN rates THEN empty map`() = runTest {
        // Arrange
        every { accountStatusListSupplier(userWalletId) } returns flowOf(accountList())

        // Act
        val actual = source.rates(userWalletId).first()

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN availability flips from Available to TemporaryUnavailable WHEN rates THEN the second map is empty`() =
        runTest {
            // Arrange
            val availability = MutableSharedFlow<StakingAvailability>(replay = 1)
            availability.tryEmit(available)
            every { accountStatusListSupplier(userWalletId) } returns flowOf(accountList(currencyStatus(solana)))
            every { stakingRepository.getStakingAvailability(userWalletId, solana) } returns availability

            // Act & Assert
            source.rates(userWalletId).test {
                assertThat(awaitItem().keys).containsExactly(solana.id)
                availability.emit(StakingAvailability.TemporaryUnavailable)
                assertThat(awaitItem()).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN a status tick without new currencies WHEN rates THEN availability is kept and activity updates`() =
        runTest {
            // Arrange
            val ticks = MutableSharedFlow<AccountStatusList>(replay = 1)
            ticks.tryEmit(accountList(currencyStatus(solana)))
            every { accountStatusListSupplier(userWalletId) } returns ticks
            every { stakingRepository.getStakingAvailability(userWalletId, solana) } returns flowOf(available)
            val staked = currencyStatus(solana, stakingBalance = MockStaking.stakeKitBalance())

            // Act & Assert
            source.rates(userWalletId).test {
                assertThat(awaitItem().getValue(solana.id).isActive).isFalse()
                ticks.emit(accountList(staked))
                assertThat(awaitItem().getValue(solana.id).isActive).isTrue()
                verify(exactly = 1) { stakingRepository.getStakingAvailability(userWalletId, solana) }
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun accountList(vararg statuses: CryptoCurrencyStatus): AccountStatusList = mockk {
        every { flattenCurrencies() } returns statuses.toList()
    }

    private fun secondAccountOf(currency: CryptoCurrency.Coin): CryptoCurrency.Coin {
        val id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkIdWithDerivationPath(
                rawId = currency.id.rawNetworkId,
                derivationPath = SECOND_ACCOUNT_PATH,
            ),
            suffix = CryptoCurrency.ID.Suffix.RawID(rawId = requireNotNull(currency.id.rawCurrencyId).value),
        )
        return currency.copy(id = id)
    }

    private companion object {
        const val SECOND_ACCOUNT_PATH = "m/44'/501'/1'/0'"
    }
}