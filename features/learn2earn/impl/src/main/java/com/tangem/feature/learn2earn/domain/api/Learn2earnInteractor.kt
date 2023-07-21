package com.tangem.feature.learn2earn.domain.api

import android.net.Uri
import com.tangem.feature.learn2earn.domain.models.PromotionError
import kotlinx.coroutines.flow.Flow

/**
 * @author Anton Zhilenkov on 07.06.2023.
 */
interface Learn2earnInteractor : WebViewRedirectHandler {

    var webViewResultHandler: WebViewResultHandler?

    suspend fun init()

    fun getIsTangemWalletFlow(): Flow<Boolean>

    fun isUserHadPromoCode(): Boolean

    fun isPromotionActive(): Boolean

    fun isPromotionActiveOnStories(): Boolean

    fun isPromotionActiveOnMain(): Boolean

    suspend fun validateUserWallet(): PromotionError?

    fun isUserRegisteredInPromotion(): Boolean

    fun getAwardAmount(): Int

    fun getAwardNetworkName(): String

    @Throws(IllegalArgumentException::class)
    suspend fun requestAward(): Result<Unit>

    fun buildUriForNewUser(): Uri

    fun buildUriForOldUser(): Uri

    fun getBasicAuthHeaders(): ArrayList<String>
}
