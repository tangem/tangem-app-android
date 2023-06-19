package com.tangem.feature.learn2earn.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.feature.learn2earn.domain.api.Learn2earnInteractor
import com.tangem.feature.learn2earn.domain.models.RedirectConsequences
import com.tangem.feature.learn2earn.domain.models.WebViewHelper
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
[REDACTED_AUTHOR]
 */
@HiltViewModel
class Learn2earnViewModel @Inject constructor(
    private val interactor: Learn2earnInteractor,
    private val router: Learn2earnRouter,
    private val dispatchers: AppCoroutineDispatcherProvider,
) : ViewModel(), WebViewHelper {

    var webViewUri: Uri = Uri.parse("https://localhost")
        private set

    fun isNeedToShowViewOnStoriesScreen(): Boolean = interactor.isNeedToShowViewOnStoriesScreen()

    fun onStoriesClick() {
        webViewUri = interactor.buildUriForStories()
        router.openWebView()
    }

    fun isNeedToShowViewOnWalletScreen(walletId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch(dispatchers.io) {
            val isNeedToShow = interactor.isNeedToShowViewOnWalletScreen(walletId)
            withContext(dispatchers.main) {
                callback(isNeedToShow)
            }
        }
    }

    fun onMainPageClick(walletId: String, cardId: String, cardPubKey: String) {
        webViewUri = interactor.buildUriForMainPage(walletId, cardId, cardPubKey)
        router.openWebView()
    }

    // region WebViewHelper
    override fun handleWebViewRedirect(uri: Uri): RedirectConsequences {
        return interactor.handleWebViewRedirect(uri)
    }

    override fun getWebViewHeaders(): Map<String, String> {
        return interactor.getBasicAuthHeaders()
    }
    // endregion WebViewHelper
}