package com.tangem.features.earn.impl.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnBannerAnalyticsEventTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN banner event WHEN built THEN id and params match the spec`(model: EventModel) {
        // Act
        val event = model.event

        // Assert
        assertThat(event.id).isEqualTo(model.expectedId)
        assertThat(event.params).isEqualTo(model.expectedParams)
    }

    private fun provideTestModels() = listOf(
        EventModel(
            event = EarnBannerAnalyticsEvent.BannerShown(
                token = "ETH",
                blockchain = "Ethereum",
                bestApy = "5.50",
                products = 3,
            ),
            expectedId = "[Earn] Earn Banner Shown",
            expectedParams = mapOf(
                "Token" to "ETH",
                "Blockchain" to "Ethereum",
                "Best APY" to "5.50",
                "Products" to "3",
            ),
        ),
        EventModel(
            event = EarnBannerAnalyticsEvent.BannerShown(
                token = "ETH",
                blockchain = "Ethereum",
                bestApy = null,
                products = 3,
            ),
            expectedId = "[Earn] Earn Banner Shown",
            expectedParams = mapOf(
                "Token" to "ETH",
                "Blockchain" to "Ethereum",
                "Products" to "3",
            ),
        ),
        EventModel(
            event = EarnBannerAnalyticsEvent.BannerEnable(token = "ETH", bestApy = "5.50"),
            expectedId = "[Earn] Button - Earn Banner",
            expectedParams = mapOf("Token" to "ETH", "Best APY" to "5.50"),
        ),
        EventModel(
            event = EarnBannerAnalyticsEvent.BannerEnable(token = "ETH", bestApy = null),
            expectedId = "[Earn] Button - Earn Banner",
            expectedParams = mapOf("Token" to "ETH"),
        ),
        EventModel(
            event = EarnBannerAnalyticsEvent.YieldRowShown(token = "ETH", activeCount = 2, positionBucket = "1K-5K"),
            expectedId = "[Earn] Yield Row Shown",
            expectedParams = mapOf("Token" to "ETH", "Active Count" to "2", "Position Bucket" to "1K-5K"),
        ),
        EventModel(
            event = EarnBannerAnalyticsEvent.YieldRowShown(token = "ETH", activeCount = 2, positionBucket = null),
            expectedId = "[Earn] Yield Row Shown",
            expectedParams = mapOf("Token" to "ETH", "Active Count" to "2"),
        ),
        EventModel(
            event = EarnBannerAnalyticsEvent.YieldRow(token = "ETH", activeCount = 2),
            expectedId = "[Earn] Button - Yield Row",
            expectedParams = mapOf("Token" to "ETH", "Active Count" to "2"),
        ),
        EventModel(
            event = EarnBannerAnalyticsEvent.YieldSheetOpened(token = "ETH", activeCount = 1, inactiveCount = 2),
            expectedId = "[Earn] Yield Sheet Opened",
            expectedParams = mapOf("Token" to "ETH", "Active Count" to "1", "Inactive Count" to "2"),
        ),
        EventModel(
            event = EarnBannerAnalyticsEvent.YieldSheetRow(product = "Vault", isActive = false, position = 3),
            expectedId = "[Earn] Button - Yield Sheet Row",
            expectedParams = mapOf("Product" to "Vault", "Row State" to "Inactive", "Position" to "3"),
        ),
        EventModel(
            event = EarnBannerAnalyticsEvent.YieldSheetRow(product = "Vault", isActive = true, position = 3),
            expectedId = "[Earn] Button - Yield Sheet Row",
            expectedParams = mapOf("Product" to "Vault", "Row State" to "Active", "Position" to "3"),
        ),
    )

    internal data class EventModel(
        val event: AnalyticsEvent,
        val expectedId: String,
        val expectedParams: Map<String, String>,
    )
}