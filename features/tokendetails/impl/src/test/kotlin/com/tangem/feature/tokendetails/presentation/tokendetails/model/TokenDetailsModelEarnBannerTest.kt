package com.tangem.feature.tokendetails.presentation.tokendetails.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.features.earn.api.overview.EarnBannerOverview
import com.tangem.features.earn.api.overview.EarnOverview
import com.tangem.features.earn.api.overview.EarnOverviews
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class TokenDetailsModelEarnBannerTest : TokenDetailsModelTestBase() {

    private val overviews = MutableStateFlow(EarnOverviews.Empty)

    @BeforeEach
    fun setUpEarn() {
        overviews.value = EarnOverviews.Empty
        every { earnOverviewSupplier(userWalletId) } returns overviews
        every { getStakingAvailabilityUseCase(any(), any()) } returns
            flowOf(StakingAvailability.TemporaryUnavailable.right())
    }

    @Test
    fun `GIVEN staking block shown WHEN Earn banner appears THEN staking block is removed`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        emitStatus(createLoadedStatus(networkSource = StatusSource.ACTUAL))
        advanceUntilIdle()
        val blockBeforeBanner = model.redesignUiState.value.earnBlockState

        // Act
        overviews.value = overviewsWithBanner()
        advanceUntilIdle()

        // Assert
        assertThat(blockBeforeBanner).isNotNull()
        assertThat(model.redesignUiState.value.earnBlockState).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN Earn banner shown WHEN banner disappears THEN staking block returns`() = runTest {
        // Arrange
        overviews.value = overviewsWithBanner()
        val model = createModel(testScope = this)
        emitStatus(createLoadedStatus(networkSource = StatusSource.ACTUAL))
        advanceUntilIdle()
        val blockWithBanner = model.redesignUiState.value.earnBlockState

        // Act
        overviews.value = EarnOverviews.Empty
        advanceUntilIdle()

        // Assert
        assertThat(blockWithBanner).isNull()
        assertThat(model.redesignUiState.value.earnBlockState).isNotNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN Earn banner of another currency WHEN model created THEN staking block shown`() = runTest {
        // Arrange
        overviews.value = overviewsWithBanner(currency = bitcoin)

        // Act
        val model = createModel(testScope = this)
        emitStatus(createLoadedStatus(networkSource = StatusSource.ACTUAL))
        advanceUntilIdle()

        // Assert
        assertThat(model.redesignUiState.value.earnBlockState).isNotNull()
        model.onDestroy()
    }

    private fun overviewsWithBanner(currency: CryptoCurrency = ethereum) = EarnOverviews(
        byCurrency = mapOf(
            currency.id to EarnOverview(
                headline = null,
                banner = EarnBannerOverview(
                    rate = null,
                    bestProductId = null,
                    activeCount = 2,
                    totalPosition = BigDecimal.ONE,
                    productCount = 2,
                ),
            ),
        ),
        source = StatusSource.ACTUAL,
    )
}