package com.tangem.common

import com.tangem.utils.SupportedLanguages

/**
[REDACTED_AUTHOR]
 */
object TangemBlogUrlBuilder {

    fun build(post: Post): String {
        val code = SupportedLanguages.getCurrentSupportedLanguageCode()
            .takeIf { code ->
                code == SupportedLanguages.RUSSIAN || code == SupportedLanguages.ENGLISH
            }
            ?: SupportedLanguages.ENGLISH

        return "https://tangem.com/$code/blog/post/${post.path}/"
    }

    sealed interface Post {

        val path: String

        data object SeedNotify : Post {
            override val path: String = "seed-notify"
        }
    }
}