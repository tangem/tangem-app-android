package com.tangem.features.earn.lending.impl.product

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.earn.api.overview.EarnBadgeAccent
import com.tangem.features.earn.lending.impl.provider.LendingProvider
import com.tangem.features.earn.lending.impl.supplier.LendingSupplier
import com.tangem.features.earn.product.api.analytics.EarnProductAnalytics
import com.tangem.features.earn.product.api.navigation.EarnFlowRoute
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.navigation.EarnNavigator
import com.tangem.features.earn.product.api.navigation.EarnPickerRoute
import com.tangem.features.earn.product.api.navigation.EarnRoute
import com.tangem.features.earn.product.api.product.EarnActionHandler
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnLegal
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnProvider
import com.tangem.features.earn.product.api.rate.EarnRateBadgeConverter
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class LendingEarnProduct @Inject constructor(
    private val lendingSupplier: LendingSupplier,
    private val lendingProvider: LendingProvider,
    private val toggles: EarnFeatureToggles,
    private val rateBadgeConverter: EarnRateBadgeConverter,
) : EarnProduct {

    private val stateMapper = LendingStateMapper(lendingProvider)
    private val summaryConverter = LendingSummaryConverter(rateBadgeConverter)

    override val id: EarnProductId = ID

    override val rank: Int = 0

    override val badgeAccent: EarnBadgeAccent = EarnBadgeAccent.Success

    override val title: TextReference = LendingTexts.title(lendingProvider.info)

    override val provider: EarnProvider = lendingProvider.info

    override val analytics: EarnProductAnalytics = EarnProductAnalytics(category = "Earn", product = "Lending")

    override val legal: EarnLegal = lendingProvider.legal

    override val howItWorks: EarnHowItWorks = lendingHowItWorks(lendingProvider.performanceFee)

    /** The registry feeds the overview headline even with Earn off, so a switched-off product must stay silent */
    private val isOffered: Boolean
        get() = toggles.isEarnEnabled && lendingProvider.isAvailable

    override fun summaries(userWalletId: UserWalletId): Flow<Map<CryptoCurrency.ID, EarnProductSummary>> {
        if (!isOffered) return flowOf(emptyMap())
        return lendingSupplier(userWalletId).map(summaryConverter::convert)
    }

    override fun state(params: EarnProduct.Params): Flow<EarnProductState> {
        if (!isOffered) return flowOf(EarnProductState.Unavailable)
        return lendingSupplier(params.userWalletId).map { snapshot ->
            val state = snapshot.assets[params.currency.id]
            val minDeposit = state?.let {
                lendingProvider.minDeposit(params.userWalletId, it.asset).getOrNull()
            }
            stateMapper.map(state, minDeposit)
        }
    }

    override suspend fun apyHistory(params: EarnProduct.Params, period: EarnPeriod): Either<Throwable, EarnApyHistory> {
        if (!isOffered) return NO_APY_HISTORY.right()
        val asset = lendingProvider.resolveAsset(params.currency) ?: return unknownAsset().left()
        return lendingProvider.apyHistory(asset, period)
    }

    override suspend fun refresh(params: EarnProduct.Params): Either<Throwable, Unit> {
        if (!isOffered) return Unit.right()
        val asset = lendingProvider.resolveAsset(params.currency) ?: return unknownAsset().left()
        return lendingProvider.refresh(params.userWalletId, asset)
    }

    override fun actionHandler(params: EarnProduct.Params, navigator: EarnNavigator): EarnActionHandler =
        EarnActionHandler { action ->
            val route = routeOf(action)
            if (route == null) {
                TangemLogger.e("Lending: unknown action ${action.id}")
            } else {
                navigator.navigate(route)
            }
        }

    private fun routeOf(action: EarnAction): EarnRoute? = when (action) {
        is EarnFlowAction -> EarnFlowRoute(productId = id, action = action)
        LendingNavigationAction.OpenPosition -> EarnInfoRoute(productId = id)
        LendingNavigationAction.OpenPicker -> EarnPickerRoute(preselected = id)
        else -> null
    }

    private fun unknownAsset() = IllegalStateException("Lending is not offered for this currency")

    companion object {
        const val ID_VALUE = "lending"
        val ID = EarnProductId(ID_VALUE)

        private val NO_APY_HISTORY = EarnApyHistory(points = emptyList(), average = null)
    }
}