package com.tangem.feature.learn2earn.domain.api

import android.net.Uri

/**
[REDACTED_AUTHOR]
 */
interface Learn2earnInteractor : WebViewRedirectHandler {

    var webViewResultHandler: WebViewResultHandler?

    fun setupDependencies(authCredentials: String?, countryCodeProvider: () -> String)

    suspend fun init()

    fun isUserHadPromoCode(): Boolean

    fun isNeedToShowViewOnStoriesScreen(): Boolean

    suspend fun isNeedToShowViewOnMainScreen(): Boolean

    fun isUserRegisteredInPromotion(): Boolean

    fun getAwardAmount(): Pair<Int, String>

    @Throws(IllegalArgumentException::class)
    suspend fun requestAward(): Result<Unit>

    fun buildUriForNewUser(): Uri

    fun buildUriForOldUser(): Uri

    fun getBasicAuthHeaders(): ArrayList<String>
}