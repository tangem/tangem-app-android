package com.tangem.features.earn.product.api.product

import arrow.core.Either
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.overview.EarnBadgeAccent
import com.tangem.features.earn.product.api.analytics.EarnProductAnalytics
import com.tangem.features.earn.product.api.navigation.EarnNavigator
import com.tangem.features.earn.product.api.session.EarnAction
import kotlinx.coroutines.flow.Flow

interface EarnProduct {

    val id: EarnProductId

    val rank: Int

    val badgeAccent: EarnBadgeAccent

    val title: TextReference

    val provider: EarnProvider

    val analytics: EarnProductAnalytics

    /**
     * The product already has its own block on the token page, and the banner must not count it, otherwise one
     * position would be shown twice. A fact about the product, not a command to the banner.
     */
    val hasLegacyBlock: Boolean
        get() = false

    fun summaries(userWalletId: UserWalletId): Flow<Map<CryptoCurrency.ID, EarnProductSummary>>

    fun state(params: Params): Flow<EarnProductState>

    /**
     * Total: provider and network errors go to `Left`, nothing is thrown. A separate call rather than a part of
     * [state]: it depends on the selected period and must not be refetched on every tick of [state].
     */
    suspend fun apyHistory(params: Params, period: EarnPeriod): Either<Throwable, EarnApyHistory>

    suspend fun refresh(params: Params): Either<Throwable, Unit>

    /**
     * The navigator lives only inside the handler and never in a product field: products are singletons, a
     * navigator lives as long as a screen.
     */
    fun actionHandler(params: Params, navigator: EarnNavigator): EarnActionHandler

    data class Params(val userWalletId: UserWalletId, val currency: CryptoCurrency)
}

fun interface EarnActionHandler {

    fun onActionClick(action: EarnAction)
}

data class EarnProvider(val title: TextReference, val icon: ImageReference)