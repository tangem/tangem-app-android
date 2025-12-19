package com.tangem.domain.wallets.usecase

import com.tangem.common.TangemSiteUrlBuilder
import java.util.Locale

class GenerateBuyTangemCardLinkUseCase {

    suspend operator fun invoke(source: Source): String {
        return invoke(source.utmCampaign)
    }

    suspend operator fun invoke(utmCampaign: String?): String {
        val langCode = Locale.getDefault().language
        val utmTags = TangemSiteUrlBuilder.getUtmTags(utmCampaign)
        return "https://buy.tangem.com/$langCode?$utmTags"
    }

    enum class Source(val utmCampaign: String) {
        Creation("prospect"),
        Settings("users"),
        Backup("backup"),
        Upgrade("upgrade"),
    }
}