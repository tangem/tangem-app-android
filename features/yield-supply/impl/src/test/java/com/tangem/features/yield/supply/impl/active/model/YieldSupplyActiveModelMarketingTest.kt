package com.tangem.features.yield.supply.impl.active.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute.YieldSupplyEntry.AccountKind
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.features.marketing.api.MarketingBannerRequest
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyActiveModelMarketingTest : YieldSupplyActiveModelTestBase() {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a flow account WHEN model created THEN marketing banner requested only in the portfolio`(
        model: MarketingModel,
    ) = runTest {
        // Arrange
        val activeModel = createModel(dispatchers = createTestingCoroutineDispatcherProvider(), account = model.account)

        // Act
        val actual = activeModel.marketingRequest.first()

        // Assert
        assertThat(actual).isEqualTo(model.expected)

        activeModel.onDestroy()
    }

    private fun provideTestModels() = listOf(
        MarketingModel(
            account = AccountKind.Portfolio,
            expected = MarketingBannerRequest(
                screen = MarketingScreen.Yield(networkId = NETWORK_ID, contractAddress = CONTRACT_ADDRESS),
            ),
        ),
        MarketingModel(account = AccountKind.Money, expected = null),
    )

    data class MarketingModel(val account: AccountKind, val expected: MarketingBannerRequest?)
}