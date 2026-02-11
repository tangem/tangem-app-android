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

        data object SeedNotify : Post {
            override val path: String = "seed-notify"
        }

        data object SeedNotifySecond : Post {
            override val path: String = "tangem-resolves-log-issue"
        }

        data object SeedPhraseRiskySolution : Post {
            override val path: String = "seed-phrase-a-risky-solution"
        }

        data object WhatWalletToChoose : Post {
            override val path: String = "mobile-wallet"
        }

        data object WhatIsTransactionFee : Post {
            override val path: String = "what-is-a-transaction-fee-and-why-do-we-need-it"
        }
    }
}