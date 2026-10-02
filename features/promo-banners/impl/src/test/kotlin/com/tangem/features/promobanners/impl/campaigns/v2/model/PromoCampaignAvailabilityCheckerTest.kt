package com.tangem.features.promobanners.impl.campaigns.v2.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.PromoCampaignStatus
import com.tangem.domain.promo.models.PromoEligibility
import com.tangem.domain.promo.models.PromoEligibilityConditions
import com.tangem.domain.promo.models.PromoWalletType
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.settings.usercountry.models.UserCountryError
import com.tangem.features.promobanners.impl.campaigns.v2.createPromoCampaign
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.info.AppInfoProvider
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PromoCampaignAvailabilityCheckerTest {

    private val appInfoProvider: AppInfoProvider = mockk()
    private val getUserCountryUseCase: GetUserCountryUseCase = mockk()

    private val checker = PromoCampaignAvailabilityChecker(
        appInfoProvider = appInfoProvider,
        getUserCountryUseCase = getUserCountryUseCase,
    )

    private val cardWallet: UserWallet.Cold = mockk {
        every { walletId } returns UserWalletId(COLD_WALLET_ID)
        every { scanResponse.card.batchId } returns BATCH_ID
    }

    private val mobileWallet: UserWallet.Hot = mockk {
        every { walletId } returns UserWalletId(HOT_WALLET_ID)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun isAvailable(model: IsAvailableModel) {
        // Arrange
        every { appInfoProvider.appVersion } returns model.appVersion
        val userCountry: Either<UserCountryError, UserCountry> =
            model.country?.let { Either.Right(UserCountry.from(it)) } ?: Either.Left(UserCountryError.NotSetup)
        every { getUserCountryUseCase.invokeSync() } returns userCountry
        val campaign = createPromoCampaign(status = model.status, eligibility = model.eligibility)

        // Act
        val actual = checker.isAvailable(campaign = campaign, userWallet = model.userWallet)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class IsAvailableModel(
        val description: String,
        val expected: Boolean,
        val status: PromoCampaignStatus = PromoCampaignStatus.Active,
        val eligibility: PromoEligibility = PromoEligibility.Public,
        val userWallet: UserWallet,
        val appVersion: String = "6.2.0",
        val country: String? = "de",
    ) {
        override fun toString() = description
    }

    private fun provideTestModels() = listOf(
        IsAvailableModel(
            description = "GIVEN active public campaign WHEN check THEN available",
            expected = true,
            userWallet = cardWallet,
        ),
        IsAvailableModel(
            description = "GIVEN paused campaign WHEN check THEN not available",
            expected = false,
            status = PromoCampaignStatus.Paused,
            userWallet = cardWallet,
        ),
        IsAvailableModel(
            description = "GIVEN draft campaign WHEN check THEN not available",
            expected = false,
            status = PromoCampaignStatus.Draft,
            userWallet = cardWallet,
        ),
        IsAvailableModel(
            description = "GIVEN archived campaign WHEN check THEN not available",
            expected = false,
            status = PromoCampaignStatus.Archived,
            userWallet = cardWallet,
        ),
        IsAvailableModel(
            description = "GIVEN private campaign without conditions WHEN check THEN available",
            expected = true,
            eligibility = createPrivate(),
            userWallet = mobileWallet,
        ),
        IsAvailableModel(
            description = "GIVEN wallet id listed in other case WHEN check THEN available",
            expected = true,
            eligibility = createPrivate(walletIds = listOf("ffff", COLD_WALLET_ID.uppercase())),
            userWallet = cardWallet,
        ),
        IsAvailableModel(
            description = "GIVEN wallet id not listed WHEN check THEN not available",
            expected = false,
            eligibility = createPrivate(walletIds = listOf("ffff")),
            userWallet = cardWallet,
        ),
        IsAvailableModel(
            description = "GIVEN card wallet type and card wallet WHEN check THEN available",
            expected = true,
            eligibility = createPrivate(walletType = PromoWalletType.Card),
            userWallet = cardWallet,
        ),
        IsAvailableModel(
            description = "GIVEN card wallet type and mobile wallet WHEN check THEN not available",
            expected = false,
            eligibility = createPrivate(walletType = PromoWalletType.Card),
            userWallet = mobileWallet,
        ),
        IsAvailableModel(
            description = "GIVEN mobile wallet type and card wallet WHEN check THEN not available",
            expected = false,
            eligibility = createPrivate(walletType = PromoWalletType.Mobile),
            userWallet = cardWallet,
        ),
        IsAvailableModel(
            description = "GIVEN mobile wallet type and mobile wallet WHEN check THEN available",
            expected = true,
            eligibility = createPrivate(walletType = PromoWalletType.Mobile),
            userWallet = mobileWallet,
        ),
        IsAvailableModel(
            description = "GIVEN card batch listed WHEN check THEN available",
            expected = true,
            eligibility = createPrivate(batchIds = listOf("AF01", BATCH_ID.lowercase())),
            userWallet = cardWallet,
        ),
        IsAvailableModel(
            description = "GIVEN card batch not listed WHEN check THEN not available",
            expected = false,
            eligibility = createPrivate(batchIds = listOf("AF01")),
            userWallet = cardWallet,
        ),
        IsAvailableModel(
            description = "GIVEN batch ids and mobile wallet WHEN check THEN not available",
            expected = false,
            eligibility = createPrivate(batchIds = listOf(BATCH_ID)),
            userWallet = mobileWallet,
        ),
        IsAvailableModel(
            description = "GIVEN user country excluded WHEN check THEN not available",
            expected = false,
            eligibility = createPrivate(countryExcluded = listOf("US", "CN")),
            userWallet = cardWallet,
            country = "us",
        ),
        IsAvailableModel(
            description = "GIVEN user country not excluded WHEN check THEN available",
            expected = true,
            eligibility = createPrivate(countryExcluded = listOf("US", "CN")),
            userWallet = cardWallet,
            country = "de",
        ),
        IsAvailableModel(
            description = "GIVEN empty excluded countries WHEN check THEN available",
            expected = true,
            eligibility = createPrivate(countryExcluded = emptyList()),
            userWallet = cardWallet,
        ),
        IsAvailableModel(
            description = "GIVEN user country unknown WHEN check THEN device locale country checked",
            expected = false,
            eligibility = createPrivate(countryExcluded = listOf(Locale.getDefault().country)),
            userWallet = cardWallet,
            country = null,
        ),
        IsAvailableModel(
            description = "GIVEN app version within bounds WHEN check THEN available",
            expected = true,
            eligibility = createPrivate(minAndroidVersion = "6.1.0", maxAndroidVersion = "6.3.0"),
            userWallet = cardWallet,
            appVersion = "6.3",
        ),
        IsAvailableModel(
            description = "GIVEN app version below min WHEN check THEN not available",
            expected = false,
            eligibility = createPrivate(minAndroidVersion = "6.1.0"),
            userWallet = cardWallet,
            appVersion = "6.0.9",
        ),
        IsAvailableModel(
            description = "GIVEN app version above max WHEN check THEN not available",
            expected = false,
            eligibility = createPrivate(maxAndroidVersion = "6.3.0"),
            userWallet = cardWallet,
            appVersion = "6.3.1",
        ),
        IsAvailableModel(
            description = "GIVEN internal build suffix WHEN check THEN version compared without it",
            expected = false,
            eligibility = createPrivate(minAndroidVersion = "6.1.0"),
            userWallet = cardWallet,
            appVersion = "6.0-internal",
        ),
        IsAvailableModel(
            description = "GIVEN unparseable app version WHEN check THEN version not checked",
            expected = true,
            eligibility = createPrivate(minAndroidVersion = "6.1.0"),
            userWallet = cardWallet,
            appVersion = "dev",
        ),
        IsAvailableModel(
            description = "GIVEN malformed version bound WHEN check THEN bound skipped",
            expected = true,
            eligibility = createPrivate(minAndroidVersion = "six", maxAndroidVersion = "6.3.0"),
            userWallet = cardWallet,
        ),
        IsAvailableModel(
            description = "GIVEN one of several conditions fails WHEN check THEN not available",
            expected = false,
            eligibility = createPrivate(
                walletIds = listOf(COLD_WALLET_ID),
                walletType = PromoWalletType.Card,
                batchIds = listOf(BATCH_ID),
                countryExcluded = listOf("DE"),
            ),
            userWallet = cardWallet,
            country = "de",
        ),
    )

    private fun createPrivate(
        walletIds: List<String>? = null,
        walletType: PromoWalletType? = null,
        batchIds: List<String>? = null,
        countryExcluded: List<String>? = null,
        minAndroidVersion: String? = null,
        maxAndroidVersion: String? = null,
    ) = PromoEligibility.Private(
        PromoEligibilityConditions(
            walletIds = walletIds,
            walletType = walletType,
            batchIds = batchIds,
            countryExcluded = countryExcluded,
            minAndroidVersion = minAndroidVersion,
            maxAndroidVersion = maxAndroidVersion,
        ),
    )

    private companion object {
        const val COLD_WALLET_ID = "00aabbcc"
        const val HOT_WALLET_ID = "00ddeeff"
        const val BATCH_ID = "AF99"
    }
}