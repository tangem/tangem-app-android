package com.tangem.domain.yield.supply.promo.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldBoostPromo
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.domain.yield.supply.promo.YieldPromoRepository
import io.mockk.Deregisterable
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.registerInstanceFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShouldShowYieldBoostMainBannerUseCaseTest {

    private val repository: YieldPromoRepository = mockk()
    private lateinit var useCase: ShouldShowYieldBoostMainBannerUseCase

    private val userWalletId = UserWalletId("abcdef012345")
    private val contractAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
    private val networkRawId = "ethereum"

    // Stub concrete instances of the sealed return types so MockK doesn't subclass them while recording coEvery
    // (Objenesis on a JVM-sealed type throws InstantiationError flakily under full-suite CI runs).
    private val instanceFactories = mutableListOf<Deregisterable>()

    @BeforeEach
    fun setUp() {
        instanceFactories += registerInstanceFactory { YieldBoostPromo.None }
        instanceFactories += registerInstanceFactory { YieldBoostStatus.NotStarted }
        useCase = ShouldShowYieldBoostMainBannerUseCase(repository = repository)
    }

    @AfterEach
    fun tearDown() {
        instanceFactories.forEach { it.deregister() }
        instanceFactories.clear()
    }

    @Test
    fun `GIVEN promo repository throws WHEN invoke THEN returns Left`() = runTest {
        coEvery { repository.getYieldBoostPromo(userWalletId, false) } throws RuntimeException("net")

        val result = useCase(userWalletId)

        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN promo is None WHEN invoke THEN returns Right(false)`() = runTest {
        coEvery { repository.getYieldBoostPromo(userWalletId, false) } returns YieldBoostPromo.None

        val result = useCase(userWalletId)

        assertThat(result.getOrNull()).isFalse()
    }

    @Test
    fun `GIVEN status repository throws WHEN invoke THEN returns Left`() = runTest {
        coEvery { repository.getYieldBoostPromo(userWalletId, false) } returns activePromo()
        coEvery { repository.getYieldBoostStatus(userWalletId, false) } throws RuntimeException("net")

        val result = useCase(userWalletId)

        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN status is Enrolled WHEN invoke THEN returns Right(false)`() = runTest {
        coEvery { repository.getYieldBoostPromo(userWalletId, false) } returns activePromo()
        coEvery { repository.getYieldBoostStatus(userWalletId, false) } returns enrolledStatus()

        val result = useCase(userWalletId)

        assertThat(result.getOrNull()).isFalse()
    }

    @Test
    fun `GIVEN promo Active and status NotStarted WHEN invoke THEN returns Right(true)`() = runTest {
        coEvery { repository.getYieldBoostPromo(userWalletId, false) } returns activePromo()
        coEvery { repository.getYieldBoostStatus(userWalletId, false) } returns YieldBoostStatus.NotStarted

        val result = useCase(userWalletId)

        assertThat(result.getOrNull()).isTrue()
    }

    private fun activePromo() = YieldBoostPromo.Active(
        tokens = listOf(
            YieldBoostPromo.Active.PromoToken(
                contractAddress = contractAddress,
                tokenSymbol = "USDC",
                tokenName = "USD Coin",
                networkId = networkRawId,
            ),
        ),
        timeline = YieldBoostPromo.Active.Timeline(
            start = Instant.parse("2026-01-01T00:00:00Z"),
            end = Instant.parse("2027-01-01T00:00:00Z"),
        ),
        link = null,
    )

    private fun enrolledStatus() = YieldBoostStatus.Enrolled(
        tokenName = "USD Coin",
        networkId = networkRawId,
        moduleAddress = "0xmodule",
        userAddress = "0xuser",
        contractAddress = contractAddress,
        qualificationEndDate = Instant.parse("2026-06-01T00:00:00Z"),
    )
}