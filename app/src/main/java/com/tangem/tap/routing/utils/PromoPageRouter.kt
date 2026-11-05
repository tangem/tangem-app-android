package com.tangem.tap.routing.utils

import android.net.Uri
import androidx.core.net.toUri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.common.uri.PromoPageUrlValidator
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.domain.marketing.PromoPageRepository
import com.tangem.domain.models.wallet.isLocked
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
    private val marketingFeatureToggles: MarketingFeatureToggles,
    private val appRouter: AppRouter,
    private val appCoroutineScope: AppCoroutineScope,
) {

    fun savePage(deeplinkUri: Uri) {
        val isPromoPageLink = deeplinkUri.scheme == DeepLinkScheme.Tangem.scheme &&
            deeplinkUri.host == DeepLinkRoute.PromoPage.host
        if (!marketingFeatureToggles.isPromoPageEnabled || !isPromoPageLink) return

        // Not getQueryParameter: it decodes + as a space
        val page = deeplinkUri.getQueryParameters(PAGE_PARAM).firstOrNull(String::isNotEmpty)
        appCoroutineScope.launch { promoPageRepository.setPage(page) }
    }

    fun observe(scope: CoroutineScope, currentRoute: Flow<AppRoute?>) {
        if (!marketingFeatureToggles.isPromoPageEnabled) return

        combine(
            promoPageRepository.observePage(),
            currentRoute,
            userWalletsListRepository.selectedUserWallet,
        ) { page, route, selectedWallet ->
            if (page != null && route is AppRoute.Wallet && selectedWallet?.isLocked == false) openPage()
        }.launchIn(scope)
    }

    private suspend fun openPage() {
        val page = promoPageRepository.consumePage() ?: return
        if (PromoPageUrlValidator.isAllowed(page.toUri())) {
            appRouter.push(AppRoute.PromoPage(page))
        } else {
            TangemLogger.i("Promo page $page is not allowed")
        }
    }

    private companion object {
        const val PAGE_PARAM = "page"
    }
}