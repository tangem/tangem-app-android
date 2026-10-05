package com.tangem.features.promobanners.impl.campaigns.v2.model

import com.tangem.core.configtoggle.version.Version
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.domain.promo.models.PromoCampaignStatus
import com.tangem.domain.promo.models.PromoEligibility
import com.tangem.domain.promo.models.PromoEligibilityConditions
import com.tangem.domain.promo.models.PromoWalletType
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.utils.info.AppInfoProvider
import java.util.Locale
import javax.inject.Inject

/**
 * Re-checks on the app side whether a campaign the backend returned may be shown for [UserWallet].
 * Campaigns are cached per wallet for the whole session, so the app version and country are checked at show time.
 */
internal class PromoCampaignAvailabilityChecker @Inject constructor(
    private val appInfoProvider: AppInfoProvider,
    private val getUserCountryUseCase: GetUserCountryUseCase,
) {

    fun isAvailable(campaign: PromoCampaign, userWallet: UserWallet): Boolean {
        if (campaign.status != PromoCampaignStatus.Active) return false

        return when (val eligibility = campaign.eligibility) {
            PromoEligibility.Public -> true
            is PromoEligibility.Private -> isEligible(eligibility.conditions, userWallet)
        }
    }

    private fun isEligible(conditions: PromoEligibilityConditions, userWallet: UserWallet): Boolean {
        return with(conditions) {
            isWalletIdMatched(walletIds, userWallet) &&
                isWalletTypeMatched(walletType, userWallet) &&
                isBatchMatched(batchIds, userWallet) &&
                isCountryAllowed(countryExcluded) &&
                isAppVersionInRange(min = minAndroidVersion, max = maxAndroidVersion)
        }
    }

    private fun isWalletIdMatched(walletIds: List<String>?, userWallet: UserWallet): Boolean {
        return walletIds?.containsIgnoringCase(userWallet.walletId.stringValue) != false
    }

    private fun isWalletTypeMatched(walletType: PromoWalletType?, userWallet: UserWallet): Boolean {
        return when (walletType) {
            null -> true
            PromoWalletType.Mobile -> userWallet is UserWallet.Hot
            PromoWalletType.Card -> userWallet is UserWallet.Cold
        }
    }

    /** A mobile wallet has no card batch, so it never matches a batch-restricted campaign. */
    private fun isBatchMatched(batchIds: List<String>?, userWallet: UserWallet): Boolean {
        if (batchIds == null) return true
        val batchId = (userWallet as? UserWallet.Cold)?.scanResponse?.card?.batchId ?: return false

        return batchIds.containsIgnoringCase(batchId)
    }

    private fun isCountryAllowed(countryExcluded: List<String>?): Boolean {
        if (countryExcluded.isNullOrEmpty()) return true
        val country = getUserCountryUseCase.invokeSync().getOrNull() ?: UserCountry.fromDeviceLocale()

        return !countryExcluded.containsIgnoringCase(country.code)
    }

    /** Internal builds carry a suffix (`6.0-internal`) the version parser doesn't accept. A malformed bound is skipped. */
    private fun isAppVersionInRange(min: String?, max: String?): Boolean {
        if (min == null && max == null) return true
        val current = Version.create(appInfoProvider.appVersion.substringBefore('-')) ?: return true

        val isAboveMin = min?.let(Version::create)?.let { current >= it } != false
        val isBelowMax = max?.let(Version::create)?.let { current <= it } != false

        return isAboveMin && isBelowMax
    }

    /** Backend identifiers are compared case-insensitively: wallet IDs are hex and the stored country is lowercase. */
    private fun List<String>.containsIgnoringCase(value: String): Boolean {
        val normalized = value.uppercase(Locale.ROOT)

        return any { it.uppercase(Locale.ROOT) == normalized }
    }
}