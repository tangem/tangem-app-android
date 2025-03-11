package com.tangem.features.markets.details.impl.ui.preview

import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.markets.details.impl.ui.state.SecurityScoreBottomSheetContent

internal object SecurityScorePreviewData {

    val bottomSheetContent = SecurityScoreBottomSheetContent(
        title = stringReference("Security score"),
        description = stringReference(
            "Security score of a token is a metric that assesses the " +
                "security level of a blockchain or token based on various factors and is compiled from " +
                "the sources listed below.",
        ),
        providers = listOf(
            SecurityScoreBottomSheetContent.SecurityScoreProviderUM(
                name = "Moralis",
                lastAuditDate = "21.10.2024",
                score = 4.9F,
                urlData = SecurityScoreBottomSheetContent.SecurityScoreProviderUM.UrlData(
                    fullUrl = "https://moralis.com/",
                    rootHost = "moralis.com",
                ),
                iconUrl = "",
            ),
            SecurityScoreBottomSheetContent.SecurityScoreProviderUM(
                name = "Certik",
                lastAuditDate = "10.07.2024",
                score = 4.6F,
                urlData = SecurityScoreBottomSheetContent.SecurityScoreProviderUM.UrlData(
                    fullUrl = "https://certik.com/",
                    rootHost = "certik.com",
                ),
                iconUrl = "",
            ),
            SecurityScoreBottomSheetContent.SecurityScoreProviderUM(
                name = "Cyberscope",
                lastAuditDate = "25.06.2023",
                score = 4.5F,
                urlData = SecurityScoreBottomSheetContent.SecurityScoreProviderUM.UrlData(
                    fullUrl = "https://cyberscope.com/",
                    rootHost = "cyberscope.com",
                ),
                iconUrl = "",
            ),
            SecurityScoreBottomSheetContent.SecurityScoreProviderUM(
                name = "TokenInsight",
                lastAuditDate = "17.01.2022",
                score = 4.0F,
                urlData = SecurityScoreBottomSheetContent.SecurityScoreProviderUM.UrlData(
                    fullUrl = "https://tokeninsight.com/",
                    rootHost = "tokeninsight.com",
                ),
                iconUrl = "",
            ),

        ),
        onProviderLinkClick = {},
    )
}