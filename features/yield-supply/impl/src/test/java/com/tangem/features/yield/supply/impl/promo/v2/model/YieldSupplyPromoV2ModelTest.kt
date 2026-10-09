package com.tangem.features.yield.supply.impl.promo.v2.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.features.yield.supply.impl.R
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.features.yield.supply.api.YieldSupplyPromoComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/** The promo screen stands on its own; only the reward chip waits on a campaign. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyPromoV2ModelTest {

    private val getTokenStatusUseCase: YieldSupplyGetTokenStatusUseCase = mockk()

    private val token = MockYieldSupply.token(symbol = "USDT")

    @BeforeEach
    fun setUp() {
        clearMocks(getTokenStatusUseCase)
        coEvery { getTokenStatusUseCase(any()) } returns MockYieldSupply.market().right()
    }

    @Test
    fun `GIVEN no campaign WHEN the promo opens THEN the screen is shown without the reward chip`() = runTest {
        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert — a missing campaign must never cost the owner the screen
        assertThat(model.uiState.value.incentiveChip).isNull()
        assertThat(model.uiState.value.tokenSymbol).isEqualTo("USDT")
    }

    @Test
    fun `GIVEN the market is unavailable WHEN the promo opens THEN the screen is shown without the reward chip`() =
        runTest {
            // Arrange
            coEvery { getTokenStatusUseCase(any()) } returns IllegalStateException("no market").left()

            // Act
            val model = createModel()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.incentiveChip).isNull()
        }

    @Test
    fun `GIVEN a campaign paying a reward APR WHEN the promo opens THEN the reward chip is added`() = runTest {
        // Arrange
        coEvery { getTokenStatusUseCase(any()) } returns MockYieldSupply.market(
            campaign = MockYieldSupply.campaign(rewardsApr = BigDecimal("1.40")),
        ).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert - the chip carries the same percent formatting as every other rate on these screens
        assertThat(model.uiState.value.incentiveChip).isEqualTo(
            resourceReference(
                id = R.string.yield_module_promo_screen_incentivised_chip,
                formatArgs = wrappedList(stringReference("1.40%")),
            ),
        )
    }

    @Test
    fun `GIVEN a campaign paying nothing WHEN the promo opens THEN the reward chip is left out`() = runTest {
        // Arrange
        coEvery { getTokenStatusUseCase(any()) } returns MockYieldSupply.market(
            campaign = MockYieldSupply.campaign(rewardsApr = BigDecimal.ZERO),
        ).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert — a chip reading "+0%" would promise a reward the campaign does not pay
        assertThat(model.uiState.value.incentiveChip).isNull()
    }

    @Test
    fun `GIVEN a dormant campaign WHEN the promo opens THEN the reward chip is left out`() = runTest {
        // Arrange
        coEvery { getTokenStatusUseCase(any()) } returns MockYieldSupply.market(
            campaign = MockYieldSupply.campaign(hasActiveCampaign = false, rewardsApr = BigDecimal("1.40")),
        ).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.incentiveChip).isNull()
    }

    private fun TestScope.createModel(currency: CryptoCurrency = token): YieldSupplyPromoV2Model =
        YieldSupplyPromoV2Model(
            paramsContainer = MutableParamsContainer(
                YieldSupplyPromoComponent.Params(
                    userWalletId = UserWalletId("011"),
                    currency = currency,
                    apy = "5.98",
                ),
            ),
            dispatchers = createTestingCoroutineDispatcherProvider(),
            yieldSupplyGetTokenStatusUseCase = getTokenStatusUseCase,
        )
}