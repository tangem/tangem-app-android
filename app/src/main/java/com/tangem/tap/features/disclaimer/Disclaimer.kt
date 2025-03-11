package com.tangem.tap.features.disclaimer

import android.net.Uri

/**
* [REDACTED_AUTHOR]
 */
interface Disclaimer {
    fun getUri(): Uri
    suspend fun accept()
    suspend fun isAccepted(): Boolean
}

abstract class BaseDisclaimer(
    private val dataProvider: DisclaimerDataProvider,
) : Disclaimer {

    val baseUrl = "https://tangem.com"

    override suspend fun accept() {
        dataProvider.accept()
    }

    override suspend fun isAccepted(): Boolean = dataProvider.isAccepted()
}

class TangemDisclaimer(dataProvider: DisclaimerDataProvider) : BaseDisclaimer(dataProvider) {
    override fun getUri(): Uri = Uri.parse("$baseUrl/tangem_tos.html")
}
