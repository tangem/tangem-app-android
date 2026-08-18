package com.tangem.features.polymarket.impl.common

/** Builds links to polymarket.com pages. */
internal object PolymarketUrlBuilder {

    private const val BASE_URL = "https://polymarket.com"

    fun build(page: Page): String = BASE_URL + page.path

    sealed interface Page {

        val path: String

        /** Public page of an event, addressed by its [slug]. */
        data class Event(val slug: String) : Page {
            override val path: String get() = "/event/$slug"
        }

        /** Polymarket terms of service. */
        data object Terms : Page {
            override val path: String = "/tos"
        }
    }
}