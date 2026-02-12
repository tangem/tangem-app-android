package com.tangem.domain.wallets.usecase

import com.tangem.common.TangemSiteUrlBuilder
import com.tangem.domain.wallets.repository.WalletsPromoRepository
import java.util.Locale

class GenerateBuyTangemCardLinkUseCase(
    private val walletsPromoRepository: WalletsPromoRepository,
) {

    suspend operator fun invoke(source: Source): String {
        return invoke(source.utmCampaign)
    }

    suspend operator fun invoke(utmCampaign: String?): String {
        val refCode = walletsPromoRepository.getReferralCodeIfExists()
        val refCodeTag = TangemSiteUrlBuilder.getRefCodeTag(refCode)
        val langCode = Locale.getDefault().language
        val utmTags = TangemSiteUrlBuilder.getUtmTags(utmCampaign)
        return "https://buy.tangem.com/$langCode?$utmTags&$refCodeTag"
    }

    enum class Source(val utmCampaign: String) {
        Creation("prospect"),
        Settings("users"),
        Backup("backup"),
        Upgrade("upgrade"),
    }
}