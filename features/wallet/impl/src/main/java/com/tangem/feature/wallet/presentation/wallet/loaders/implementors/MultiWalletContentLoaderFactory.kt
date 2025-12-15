package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.nft.GetNFTCollectionsUseCase
import com.tangem.domain.promo.GetStoryContentUseCase
import com.tangem.domain.staking.usecase.StakingApyFlowUseCase
import com.tangem.domain.tokens.ApplyTokenListSortingUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsSingleEventSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.MultiWalletTokenListStore
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.subscribers.TangemPayMainSubscriber
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.tangempay.TangemPayFeatureToggles
import javax.inject.Inject

@Suppress("LongParameterList")
@Deprecated("Use MultiWalletContentLoaderV2.Factory instead")
@ModelScoped
internal class MultiWalletContentLoaderFactory @Inject constructor(
    private val stateHolder: WalletStateController,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val getMultiWalletWarningsFactory: GetMultiWalletWarningsFactory,
    private val tokenListStore: MultiWalletTokenListStore,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val applyTokenListSortingUseCase: ApplyTokenListSortingUseCase,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val walletWarningsSingleEventSender: WalletWarningsSingleEventSender,
    private val shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val getStoryContentUseCase: GetStoryContentUseCase,
    private val walletsRepository: WalletsRepository,
    private val getNFTCollectionsUseCase: GetNFTCollectionsUseCase,
    private val currenciesRepository: CurrenciesRepository,
    private val yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase,
    private val stakingApyFlowUseCase: StakingApyFlowUseCase,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
    private val tangemPayMainSubscriberFactory: TangemPayMainSubscriber.Factory,
) {

    fun create(userWallet: UserWallet, clickIntents: WalletClickIntents): WalletContentLoader {
        return MultiWalletContentLoader(
            userWallet = userWallet,
            clickIntents = clickIntents,
            stateHolder = stateHolder,
            tokenListAnalyticsSender = tokenListAnalyticsSender,
            walletWithFundsChecker = walletWithFundsChecker,
            tokenListStore = tokenListStore,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getMultiWalletWarningsFactory = getMultiWalletWarningsFactory,
            walletWarningsAnalyticsSender = walletWarningsAnalyticsSender,
            walletWarningsSingleEventSender = walletWarningsSingleEventSender,
            applyTokenListSortingUseCase = applyTokenListSortingUseCase,
            getStoryContentUseCase = getStoryContentUseCase,
            shouldSaveUserWalletsUseCase = shouldSaveUserWalletsUseCase,
            walletsRepository = walletsRepository,
            getNFTCollectionsUseCase = getNFTCollectionsUseCase,
            currenciesRepository = currenciesRepository,
            yieldSupplyApyFlowUseCase = yieldSupplyApyFlowUseCase,
            stakingApyFlowUseCase = stakingApyFlowUseCase,
            hotWalletFeatureToggles = hotWalletFeatureToggles,
            tangemPayFeatureToggles = tangemPayFeatureToggles,
            tangemPayMainSubscriberFactory = tangemPayMainSubscriberFactory,
        )
    }
}