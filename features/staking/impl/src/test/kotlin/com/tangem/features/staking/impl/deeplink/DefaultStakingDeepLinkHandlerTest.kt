package com.tangem.features.staking.impl.deeplink

import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.staking.GetStakingAvailabilityUseCase
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class DefaultStakingDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk(relaxed = true)
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()
    private val supplier: MultiWalletCryptoCurrenciesSupplier = mockk()
    private val availabilityUseCase: GetStakingAvailabilityUseCase = mockk()

    private val wallet = MockUserWalletFactory.create()
    private val currency = coin(Blockchain.Solana)
    private val option = StakingOption.StakeKit(StakingIntegrationID.StakeKit.Coin.Solana, MockStaking.solanaYield)
    private val params = mapOf(
        NETWORK_ID_KEY to currency.network.rawId.uppercase(),
        TOKEN_ID_KEY to requireNotNull(currency.id.rawCurrencyId).value.uppercase(),
    )
    private val expectedRoute = AppRoute.Staking(
        userWalletId = wallet.walletId,
        cryptoCurrency = currency,
        integrationId = StakingIntegrationID.StakeKit.Coin.Solana,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(appRouter, getSelectedWalletSyncUseCase, supplier, availabilityUseCase)
        every { getSelectedWalletSyncUseCase() } returns wallet.right()
        coEvery {
            supplier.getSyncOrNull(MultiWalletCryptoCurrenciesProducer.Params(wallet.walletId), any())
        } returns setOf(currency)
        coEvery { availabilityUseCase.invokeSync(wallet.walletId, currency) } returns
            StakingAvailability.Available(option).right()
    }

    @Test
    fun `GIVEN matching currency case insensitive and available WHEN created THEN staking route pushed`() = runTest {
        // Act
        createHandler(params)

        // Assert
        verify(exactly = 1) { appRouter.push(expectedRoute, any()) }
    }

    @Test
    fun `GIVEN full availability WHEN created THEN staking route pushed`() = runTest {
        // Arrange
        coEvery { availabilityUseCase.invokeSync(any(), any()) } returns StakingAvailability.Full(option).right()

        // Act
        createHandler(params)

        // Assert
        verify(exactly = 1) { appRouter.push(expectedRoute, any()) }
    }

    @Test
    fun `GIVEN ethereum with p2p pool option WHEN created THEN staking route pushed with p2p integration`() = runTest {
        // Arrange
        val ethereum = coin(Blockchain.Ethereum)
        coEvery { supplier.getSyncOrNull(any(), any()) } returns setOf(currency, ethereum)
        coEvery { availabilityUseCase.invokeSync(wallet.walletId, ethereum) } returns
            StakingAvailability.Available(StakingOption.P2PEthPool(listOf(MockStaking.vault()))).right()
        val ethereumParams = mapOf(
            NETWORK_ID_KEY to ethereum.network.rawId,
            TOKEN_ID_KEY to requireNotNull(ethereum.id.rawCurrencyId).value,
        )

        // Act
        createHandler(ethereumParams)

        // Assert
        verify(exactly = 1) {
            appRouter.push(
                AppRoute.Staking(
                    userWalletId = wallet.walletId,
                    cryptoCurrency = ethereum,
                    integrationId = StakingIntegrationID.P2PEthPool,
                ),
                any(),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UnavailableStaking {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN unavailable staking WHEN created THEN nothing pushed`(model: UnavailableModel) = runTest {
            // Arrange
            coEvery { availabilityUseCase.invokeSync(any(), any()) } returns model.availability.right()

            // Act
            createHandler(params)

            // Assert
            verifyNothingPushed()
        }

        private fun provideTestModels() = listOf(
            UnavailableModel(StakingAvailability.Unavailable),
            UnavailableModel(StakingAvailability.TemporaryUnavailable),
            UnavailableModel(StakingAvailability.RegionUnavailable),
        )
    }

    @Test
    fun `GIVEN availability error WHEN created THEN nothing pushed`() = runTest {
        // Arrange
        coEvery { availabilityUseCase.invokeSync(any(), any()) } returns StakingError.DomainError("x").left()

        // Act
        createHandler(params)

        // Assert
        verifyNothingPushed()
    }

    @Test
    fun `GIVEN no selected wallet WHEN created THEN nothing loaded or pushed`() = runTest {
        // Arrange
        every { getSelectedWalletSyncUseCase() } returns GetUserWalletError.UserWalletNotFound.left()

        // Act
        createHandler(params)

        // Assert
        verifyNothingPushed()
        coVerify(exactly = 0) { supplier.getSyncOrNull(any(), any()) }
    }

    @Test
    fun `GIVEN wallet id of other wallet WHEN created THEN ignored`() = runTest {
        // Act
        createHandler(params + (WALLET_ID_KEY to "ffff"))

        // Assert
        verifyNothingPushed()
        coVerify(exactly = 0) { supplier.getSyncOrNull(any(), any()) }
    }

    @Test
    fun `GIVEN wallet id of selected wallet WHEN created THEN staking route pushed`() = runTest {
        // Act
        createHandler(params + (WALLET_ID_KEY to wallet.walletId.stringValue))

        // Assert
        verify(exactly = 1) { appRouter.push(expectedRoute, any()) }
    }

    @Test
    fun `GIVEN supplier returns null WHEN created THEN nothing pushed`() = runTest {
        // Arrange
        coEvery { supplier.getSyncOrNull(any(), any()) } returns null

        // Act
        createHandler(params)

        // Assert
        verifyNothingPushed()
        coVerify(exactly = 0) { availabilityUseCase.invokeSync(any(), any()) }
    }

    @Test
    fun `GIVEN network mismatch WHEN created THEN nothing pushed`() = runTest {
        // Act
        createHandler(params + (NETWORK_ID_KEY to "bitcoin"))

        // Assert
        verifyNothingPushed()
    }

    @Test
    fun `GIVEN token mismatch WHEN created THEN nothing pushed`() = runTest {
        // Act
        createHandler(params + (TOKEN_ID_KEY to "other"))

        // Assert
        verifyNothingPushed()
    }

    @Test
    fun `GIVEN missing query params WHEN created THEN nothing pushed`() = runTest {
        // Act
        createHandler(emptyMap())

        // Assert
        verifyNothingPushed()
    }

    private fun TestScope.createHandler(queryParams: Map<String, String>) {
        DefaultStakingDeepLinkHandler(
            scope = this,
            queryParams = queryParams,
            appRouter = appRouter,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            multiWalletCryptoCurrenciesSupplier = supplier,
            getStakingAvailabilityUseCase = availabilityUseCase,
        )
        advanceUntilIdle()
    }

    private fun verifyNothingPushed() {
        verify(exactly = 0) { appRouter.push(any(), any()) }
    }

    internal data class UnavailableModel(val availability: StakingAvailability)
}