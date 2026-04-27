package com.tangem.common

/**
[REDACTED_AUTHOR]
 */
object TangemBlogUrlBuilder {

    suspend fun build(post: Post): String {
        return TangemSiteUrlBuilder.url(
            path = "/blog/post/${post.path}/",
            campaign = "articles",
        )
    }

    sealed interface Post {

        val path: String

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
    }
}