package com.tangem.tap.routing.utils

import android.net.Uri
import androidx.core.net.toUri
import com.tangem.common.routing.AppRoute
import com.tangem.common.uri.PromoPageUrlValidator
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.marketing.PromoPageRepository
import com.tangem.domain.models.wallet.isLocked
import com.tangem.features.marketing.api.promopage.PromoPageDeepLinkHandler
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class PromoPageRouter @Inject constructor(
    private val promoPageRepository: PromoPageRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val appCoroutineScope: AppCoroutineScope,
) {

    fun savePage(deeplinkUri: Uri) {
        if (!PromoPageDeepLinkHandler.canHandle(deeplinkUri)) return

        // Not getQueryParameter: it decodes + as a space
        val page = deeplinkUri.getQueryParameters(PAGE_PARAM).firstOrNull(String::isNotEmpty)
        appCoroutineScope.launch { promoPageRepository.setPage(page) }
    }

    fun observe(scope: CoroutineScope, currentRoute: Flow<AppRoute?>, router: Router) {
        combine(
            promoPageRepository.observePage(),
            currentRoute,
            userWalletsListRepository.selectedUserWallet,
        ) { page, route, selectedWallet ->
            if (page != null && route is AppRoute.Wallet && selectedWallet?.isLocked == false) openPage(router)
        }.launchIn(scope)
    }

    private suspend fun openPage(router: Router) {
        val page = promoPageRepository.consumePage() ?: return
        if (PromoPageUrlValidator.isAllowed(page.toUri())) {
            router.push(AppRoute.PromoPage(page))
        } else {
            TangemLogger.i("Promo page $page is not allowed")
        }
    }

    private companion object {
        const val PAGE_PARAM = "page"
    }
}