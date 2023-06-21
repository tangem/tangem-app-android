package com.tangem.feature.learn2earn.domain.api

import android.net.Uri
import com.tangem.feature.learn2earn.domain.models.RedirectConsequences

/**
 * @author Anton Zhilenkov on 07.06.2023.
 */
interface Learn2earnInteractor {

    suspend fun init()

    fun isNeedToShowViewOnStoriesScreen(): Boolean

    suspend fun isNeedToShowViewOnWalletScreen(walletId: String): Boolean

    fun getBasicAuthHeaders(): Map<String, String>

    fun buildUriForStories(): Uri

    fun buildUriForMainPage(walletId: String, cardId: String, cardPubKey: String): Uri

    fun handleWebViewRedirect(uri: Uri): RedirectConsequences
}
