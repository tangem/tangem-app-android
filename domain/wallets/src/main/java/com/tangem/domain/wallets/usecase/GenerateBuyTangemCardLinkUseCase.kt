package com.tangem.domain.wallets.usecase

import com.tangem.common.TangemSiteUrlBuilder
import com.tangem.domain.wallets.repository.WalletsPromoRepository
import com.tangem.utils.SupportedLanguages

class GenerateBuyTangemCardLinkUseCase(
    private val walletsPromoRepository: WalletsPromoRepository,
    private val tangemSiteUrlBuilder: TangemSiteUrlBuilder,
) {

    suspend operator fun invoke(source: Source): String {
        return invoke(source.utmCampaign)
    }

    suspend operator fun invoke(utmCampaign: String?): String {
        val refCode = walletsPromoRepository.getReferralCodeIfExists()
        val refCodeTag = tangemSiteUrlBuilder.getRefCodeTag(refCode)
        val langCode = SupportedLanguages.getCurrentSupportedLanguageCode()
        val utmTags = tangemSiteUrlBuilder.getUtmTags(utmCampaign)
        return "https://buy.tangem.com/$langCode?$utmTags&$refCodeTag"
    }

    enum class Source(val utmCampaign: String) {
        Creation("prospect"),
        Settings("users"),
        Backup("backup"),
        Upgrade("upgrade"),
        Welcome("welcome"),
    }
}