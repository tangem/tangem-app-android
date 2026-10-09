package com.tangem.features.promobanners.impl.campaigns.v2.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.features.promobanners.api.PromoBannersBlockComponent.Placeholder
import com.tangem.features.promobanners.impl.campaigns.v2.createPromoCampaign
import com.tangem.features.promobanners.impl.model.PromoBannerDisplay
import com.tangem.features.promobanners.impl.model.PromoBannerPriority
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PromoBannerCampaignMatcherTest {

    private val userWallet: UserWallet = mockk()

    private val matcher = PromoBannerCampaignMatcher()

    @ParameterizedTest
    @ProvideTestModels
    fun filter(model: FilterModel) {
        // Arrange
        val banner = createBanner(campaignId = model.bannerCampaignId)

        // Act
        val actual = matcher.filter(banners = listOf(banner), walletCampaigns = model.walletCampaigns)

        // Assert
        assertThat(actual).isEqualTo(if (model.isShown) listOf(banner) else emptyList())
    }

    data class FilterModel(
        val description: String,
        val bannerCampaignId: Int?,
        val walletCampaigns: WalletCampaigns?,
        val isShown: Boolean,
    ) {
        override fun toString() = description
    }

    private fun provideTestModels() = listOf(
        FilterModel(
            description = "GIVEN banner without campaign and campaigns failed WHEN filter THEN shown",
            bannerCampaignId = null,
            walletCampaigns = WalletCampaigns.Failed,
            isShown = true,
        ),
        FilterModel(
            description = "GIVEN campaign banner and campaigns loading WHEN filter THEN hidden",
            bannerCampaignId = CAMPAIGN_ID,
            walletCampaigns = null,
            isShown = false,
        ),
        FilterModel(
            description = "GIVEN campaign banner and campaigns failed WHEN filter THEN hidden",
            bannerCampaignId = CAMPAIGN_ID,
            walletCampaigns = WalletCampaigns.Failed,
            isShown = false,
        ),
        FilterModel(
            description = "GIVEN matching campaign not enrolled WHEN filter THEN shown",
            bannerCampaignId = CAMPAIGN_ID,
            walletCampaigns = loaded(createPromoCampaign(id = CAMPAIGN_ID)),
            isShown = true,
        ),
        FilterModel(
            description = "GIVEN matching campaign already enrolled WHEN filter THEN hidden",
            bannerCampaignId = CAMPAIGN_ID,
            walletCampaigns = loaded(createPromoCampaign(id = CAMPAIGN_ID, isAlreadyEnrolled = true)),
            isShown = false,
        ),
        FilterModel(
            description = "GIVEN no campaign with banner id WHEN filter THEN hidden",
            bannerCampaignId = CAMPAIGN_ID,
            walletCampaigns = loaded(createPromoCampaign(id = OTHER_CAMPAIGN_ID)),
            isShown = false,
        ),
    )

    private fun loaded(vararg campaigns: PromoCampaign) = WalletCampaigns.Loaded(userWallet, campaigns.toList())

    private fun createBanner(campaignId: Int?) = PromoBannerDisplay(
        id = 1,
        placeholder = Placeholder.MAIN.value,
        priority = PromoBannerPriority.MEDIUM,
        title = "Title",
        subtitle = "Subtitle",
        iconUrl = null,
        deeplink = null,
        isButtonEnabled = false,
        buttonText = null,
        isDismissable = false,
        campaignId = campaignId,
    )

    private companion object {
        const val CAMPAIGN_ID = 42
        const val OTHER_CAMPAIGN_ID = 7
    }
}