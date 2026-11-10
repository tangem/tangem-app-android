package com.tangem.features.earn.impl.fake

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.overview.EarnBadgeAccent
import com.tangem.features.earn.product.api.analytics.EarnProductAnalytics
import com.tangem.features.earn.product.api.navigation.EarnFlowRoute
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.navigation.EarnNavigator
import com.tangem.features.earn.product.api.navigation.EarnPickerRoute
import com.tangem.features.earn.product.api.navigation.EarnRoute
import com.tangem.features.earn.product.api.product.EarnActionHandler
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnProvider
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.features.earn.product.api.session.EarnFlowAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class FakeEarnProduct(
    private val preset: FakeEarnProductPreset,
    private val behavior: FakeEarnProductBehavior,
) : EarnProduct {

    private val spec = preset.spec

    override val id: EarnProductId = spec.id

    override val rank: Int = spec.rank

    override val badgeAccent: EarnBadgeAccent = spec.badgeAccent

    override val title: TextReference = spec.title

    override val provider: EarnProvider = spec.provider

    override val analytics: EarnProductAnalytics = spec.analytics

    override val hasLegacyBlock: Boolean = spec.hasLegacyBlock

    override fun summaries(userWalletId: UserWalletId): Flow<Map<CryptoCurrency.ID, EarnProductSummary>> = flowOf(
        mapOf(CryptoCurrency.ID.fromValue(FAKE_ETH_CURRENCY_ID) to fakeSummary(preset, behavior.position)),
    )

    override fun state(params: EarnProduct.Params): Flow<EarnProductState> =
        flowOf(fakeReadyState(preset, behavior.position))

    override suspend fun apyHistory(params: EarnProduct.Params, period: EarnPeriod): Either<Throwable, EarnApyHistory> =
        if (preset == FakeEarnProductPreset.Vault) IllegalStateException("fake").left() else fakeApyHistory(period)

    override suspend fun refresh(params: EarnProduct.Params): Either<Throwable, Unit> = Unit.right()

    override fun actionHandler(params: EarnProduct.Params, navigator: EarnNavigator): EarnActionHandler =
        if (preset == FakeEarnProductPreset.Staking) {
            EarnActionHandler { navigator.navigate(FakeForeignRoute) }
        } else {
            EarnActionHandler { action -> navigator.navigate(routeOf(action)) }
        }

    private fun routeOf(action: EarnAction): EarnRoute = when {
        action is EarnFlowAction -> EarnFlowRoute(productId = id, action = action)
        action == FakeNavigationAction.OpenInfo -> EarnInfoRoute(productId = id)
        action == FakeNavigationAction.OpenPicker -> EarnPickerRoute(preselected = id)
        else -> error("Unknown action ${action.id}")
    }
}