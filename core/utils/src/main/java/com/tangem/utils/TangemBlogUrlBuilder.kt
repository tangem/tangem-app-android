package com.tangem.utils

import java.util.Locale

@Deprecated("Use TangemBlogUrlBuilder from common module")
object TangemBlogUrlBuilder {

    private const val RU_LOCALE = "ru"
    private const val EN_LOCALE = "en"

    private const val TANGEM_MAIN = "https://tangem.com/"

    val FEE_BLOG_LINK: String
        get(): String {
            val locale = if (Locale.getDefault().language == RU_LOCALE) RU_LOCALE else EN_LOCALE
            return buildString {
                append(TANGEM_MAIN)
                append(locale)
                append("/blog/post/what-is-a-transaction-fee-and-why-do-we-need-it/")
            }
        }

    const val RESOURCE_TO_LEARN_ABOUT_APPROVING_IN_SWAP = "https://tangem.com/en/blog/post/give-revoke-permission/"

    const val YIELD_SUPPLY_HOW_IT_WORKS_URL = "https://tangem.com/en/blog/post/yield-mode"
    const val YIELD_SUPPLY_TOS_URL = "https://aave.com/terms-of-service"
    const val YIELD_SUPPLY_PRIVACY_URL = "https://aave.com/privacy-policy"
}