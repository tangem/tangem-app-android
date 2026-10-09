package com.tangem.features.promobanners.impl.campaigns.v2.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.PromoEnrollment
import com.tangem.domain.promo.models.PromoMatchMode
import com.tangem.domain.promo.models.PromoWalletType
import com.tangem.features.promobanners.impl.campaigns.v2.createPromoCampaign
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class PromoEnrollmentsFactoryTest {

    private val cardWallet = MockUserWalletFactory.create()
    private val mobileWallet = UserWallet.Hot(
        name = "Mobile",
        walletId = UserWalletId("0b"),
        hotWalletId = mockk(),
        wallets = null,
        backedUp = true,
    )
    private val wallets = listOf(cardWallet, mobileWallet)
    private val payout = PromoEnrollment.PromoPayoutTarget(
        userAddress = "0xUser",
        tokenAddress = "0xToken",
        networkId = "ethereum",
        tokenId = "tether",
    )

    @Test
    fun `GIVEN strict campaign WHEN create THEN only main wallet enrolled`() {
        // Arrange
        val campaign = createPromoCampaign(matchMode = PromoMatchMode.Strict, isPayoutNeeded = true)

        // Act
        val actual = PromoEnrollmentsFactory.create(campaign, mobileWallet.walletId, wallets, payout)

        // Assert
        assertThat(actual).containsExactly(
            PromoEnrollment(
                userWalletId = mobileWallet.walletId,
                walletType = PromoWalletType.Mobile,
                batchId = null,
                payout = payout,
                isMain = true,
            ),
        )
    }

    @Test
    fun `GIVEN linked campaign WHEN create THEN every wallet enrolled and only opener is main`() {
        // Arrange
        val campaign = createPromoCampaign(matchMode = PromoMatchMode.LinkedWallet, isPayoutNeeded = true)

        // Act
        val actual = PromoEnrollmentsFactory.create(campaign, mobileWallet.walletId, wallets, payout)

        // Assert
        assertThat(actual).containsExactly(
            PromoEnrollment(
                userWalletId = cardWallet.walletId,
                walletType = PromoWalletType.Card,
                batchId = cardWallet.scanResponse.card.batchId,
                payout = payout,
                isMain = false,
            ),
            PromoEnrollment(
                userWalletId = mobileWallet.walletId,
                walletType = PromoWalletType.Mobile,
                batchId = null,
                payout = payout,
                isMain = true,
            ),
        ).inOrder()
    }

    @Test
    fun `GIVEN linked campaign and payout from non-opener wallet WHEN create THEN every enrollment carries that payout`() {
        // Arrange
        val campaign = createPromoCampaign(matchMode = PromoMatchMode.LinkedWallet, isPayoutNeeded = true)
        val cardWalletPayout = payout.copy(userAddress = "0xCardWallet")

        // Act
        val actual = PromoEnrollmentsFactory.create(campaign, mobileWallet.walletId, wallets, cardWalletPayout)

        // Assert
        assertThat(actual.map { it.payout }).containsExactly(cardWalletPayout, cardWalletPayout)
    }

    @Test
    fun `GIVEN payout not needed WHEN create THEN payout omitted`() {
        // Arrange
        val campaign = createPromoCampaign(isPayoutNeeded = false)

        // Act
        val actual = PromoEnrollmentsFactory.create(campaign, cardWallet.walletId, wallets, payout)

        // Assert
        assertThat(actual.single().payout).isNull()
    }
}