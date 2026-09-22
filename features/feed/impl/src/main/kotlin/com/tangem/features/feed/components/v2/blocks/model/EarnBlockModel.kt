package com.tangem.features.feed.components.v2.blocks.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.usecase.GetTopEarnTokensUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.usecase.StakingAvailabilityListUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.features.feed.components.v2.blocks.FeedBlockModalCallbacks
import com.tangem.features.feed.components.v2.model.converter.EarnBlockConverter
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.ui.v2.state.EarnBlockUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import javax.inject.Inject

/**
 * What the Earn pitch says and where it leads, both following the user's own earning — see
 * [EarnBlockUM].
 *
 * The pitch is drawn from the holdings of every unlocked wallet, not just the selected one, matching
 * the For You screen it can lead to.
 */
@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class EarnBlockModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val router: Router,
    userWalletsListRepository: UserWalletsListRepository,
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    private val stakingAvailabilityListUseCase: StakingAvailabilityListUseCase,
    yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase,
    getTopEarnTokensUseCase: GetTopEarnTokensUseCase,
) : Model() {

    private val callbacks = paramsContainer.require<FeedBlockModalCallbacks>()

    val state: StateFlow<EarnBlockUM> = combine(
        flow = userWalletsListRepository.userWallets,
        flow2 = multiAccountStatusListSupplier.invokeAsMap(),
        flow3 = yieldSupplyApyFlowUseCase(),
        flow4 = getTopEarnTokensUseCase(),
    ) { wallets, accountsByWallet, yieldSupplyApy, topEarnTokens ->
        val unlockedWalletIds = wallets.orEmpty().filterNot { it.isLocked }.mapTo(mutableSetOf()) { it.walletId }
        val currenciesByWallet = accountsByWallet
            .filterKeys { walletId -> walletId in unlockedWalletIds }
            .mapValues { (_, accounts) -> accounts.flattenPersonalCurrencies() }

        EarnBlockConverter(
            yieldSupplyApy = yieldSupplyApy,
            stakingAvailability = stakingAvailabilityOf(currenciesByWallet),
            catalogueApy = topEarnTokens?.getOrNull()?.bestApy(),
        ).convert(currenciesByWallet.values.flatten())
    }
        .flowOn(dispatchers.default)
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = EarnBlockUM())

    fun onClick() {
        when (state.value.target) {
            EarnBlockUM.Target.EarnTab -> callbacks.onOpenTab(FeedTabId.Earn)
            EarnBlockUM.Target.ForYou -> router.push(FeedRoute.ForYou)
        }
    }

    private suspend fun stakingAvailabilityOf(
        currenciesByWallet: Map<UserWalletId, List<CryptoCurrencyStatus>>,
    ): Map<CryptoCurrency, StakingAvailability> = currenciesByWallet
        .flatMap { (walletId, statuses) ->
            stakingAvailabilityListUseCase.invokeSync(
                userWalletId = walletId,
                cryptoCurrencyList = statuses.map { it.currency },
            ).entries
        }
        .associate { it.key to it.value }

    /**
     * Best rate the Earn catalogue offers. Unlike the yield-supply rates, the catalogue reports a
     * fraction already (`0.0384` is 3.84%), so it reaches the percent format untouched.
     */
    private fun List<EarnTokenWithCurrency>.bestApy(): BigDecimal? = mapNotNull {
        it.earnToken.apy.toBigDecimalOrNull()
    }
        .maxOrNull()
}