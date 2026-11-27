package com.tangem.common

import javax.inject.Inject

/**
[REDACTED_AUTHOR]
 */
class TangemBlogUrlBuilder @Inject constructor(
    private val tangemSiteUrlBuilder: TangemSiteUrlBuilder,
) {

    suspend fun build(post: Post): String {
        return tangemSiteUrlBuilder.url(
            path = post.sitePath,
            campaign = "articles",
        )
    }

    sealed interface Post {

        val path: String

        val sitePath: String
            get() = "/embed/blog/post/$path/"

        data object SeedPhraseRiskySolution : Post {
            override val path: String = "seed-phrase-faq"
        }

        data object WhatWalletToChoose : Post {
            override val path: String = "mobile-wallet"
        }

        data object WhatIsTransactionFee : Post {
            override val path: String = "what-is-a-transaction-fee-and-why-do-we-need-it"
        }

        data object HowToScan : Post {
            override val path: String = "scan-tangem-card"
        }

        data object HowToStake : Post {
            override val path: String = "how-to-stake-cryptocurrency"
        }

        data object GiveRevokePermission : Post {
            override val path: String = "give-revoke-permission"
        }

        data object HowYieldModeWorks : Post {
            override val path: String = "yield-mode"
        }

        data object AboutCrossChainBridges : Post {
            override val path: String = "an-overview-of-cross-chain-bridges"
        }

        // There is no /embed/blog page for help center articles
        data object LendingOnAave : Post {
            override val path: String = "lending-on-aave"
            override val sitePath: String = "/en/help-center/vaults/$path/"
        }
    }
}