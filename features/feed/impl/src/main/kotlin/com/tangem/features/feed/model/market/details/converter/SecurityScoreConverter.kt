package com.tangem.features.feed.model.market.details.converter

import androidx.compose.runtime.Stable
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.model.market.details.formatter.MarketsDateTimeFormatters
import com.tangem.features.feed.ui.market.detailed.state.SecurityScoreBottomSheetContent
import com.tangem.features.feed.ui.market.detailed.state.SecurityScoreUM
import com.tangem.utils.converter.Converter

@Stable
internal class SecurityScoreConverter(
    private val onSecurityScoreInfoClick: (SecurityScoreBottomSheetContent) -> Unit,
    private val onSecurityScoreProviderLinkClick: (SecurityScoreBottomSheetContent.SecurityScoreProviderUM) -> Unit,
) : Converter<TokenMarketInfo.SecurityData, SecurityScoreUM> {

    override fun convert(value: TokenMarketInfo.SecurityData): SecurityScoreUM {
        val ratingsCount = value.securityScoreProviderData.size
        return SecurityScoreUM(
            score = value.totalSecurityScore,
            description = pluralReference(
                id = R.plurals.markets_token_details_based_on_ratings,
                count = ratingsCount,
                formatArgs = wrappedList(ratingsCount),
            ),
            onInfoClick = {
                onSecurityScoreInfoClick(
                    SecurityScoreBottomSheetContent(
                        title = resourceReference(R.string.markets_token_details_security_score),
                        description = resourceReference(R.string.markets_token_details_security_score_description),
                        providers = value.securityScoreProviderData.map { securityScoreProvider ->
                            SecurityScoreBottomSheetContent.SecurityScoreProviderUM(
                                name = securityScoreProvider.providerName,
                                lastAuditDate = securityScoreProvider.lastAuditDate?.let { date ->
                                    MarketsDateTimeFormatters.formatAsDate(date.millis)
                                },
                                score = securityScoreProvider.securityScore,
                                urlData = securityScoreProvider.urlData?.let { urlData ->
                                    SecurityScoreBottomSheetContent.SecurityScoreProviderUM.UrlData(
                                        fullUrl = urlData.fullUrl,
                                        rootHost = urlData.rootHost,
                                    )
                                },
                                iconUrl = securityScoreProvider.iconUrl,
                            )
                        },
                        onProviderLinkClick = onSecurityScoreProviderLinkClick,
                    ),
                )
            },
        )
    }
}