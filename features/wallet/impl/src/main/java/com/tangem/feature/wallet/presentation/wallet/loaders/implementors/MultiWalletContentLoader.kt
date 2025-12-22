package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.nft.GetNFTCollectionsUseCase
import com.tangem.domain.promo.GetStoryContentUseCase
import com.tangem.domain.staking.usecase.StakingAvailabilityListUseCase
import com.tangem.domain.tokens.ApplyTokenListSortingUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetShouldShowMainPromoUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsSingleEventSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.MultiWalletTokenListStore
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.subscribers.*
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.tangempay.TangemPayFeatureToggles

@Suppress("LongParameterList")
@Deprecated("Use MultiWalletContentLoaderV2 instead")
@ModelScoped
internal class MultiWalletContentLoader(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val walletWarningsSingleEventSender: WalletWarningsSingleEventSender,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val tokenListStore: MultiWalletTokenListStore,
    private val getNFTCollectionsUseCase: GetNFTCollectionsUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val applyTokenListSortingUseCase: ApplyTokenListSortingUseCase,
    private val getMultiWalletWarningsFactory: GetMultiWalletWarningsFactory,
    private val shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val getStoryContentUseCase: GetStoryContentUseCase,
    private val walletsRepository: WalletsRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase,
    private val stakingAvailabilityListUseCase: StakingAvailabilityListUseCase,
    private val yieldSupplyGetShouldShowMainPromoUseCase: YieldSupplyGetShouldShowMainPromoUseCase,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
    private val tangemPayMainSubscriberFactory: TangemPayMainSubscriber.Factory,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> {
        return buildList {
            MultiWalletTokenListSubscriber(
                userWallet = userWallet,
                stateHolder = stateHolder,
                clickIntents = clickIntents,
                tokenListAnalyticsSender = tokenListAnalyticsSender,
                walletWithFundsChecker = walletWithFundsChecker,
                tokenListStore = tokenListStore,
                getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
                applyTokenListSortingUseCase = applyTokenListSortingUseCase,
                yieldSupplyApyFlowUseCase = yieldSupplyApyFlowUseCase,
                stakingAvailabilityListUseCase = stakingAvailabilityListUseCase,
                yieldSupplyGetShouldShowMainPromoUseCase = yieldSupplyGetShouldShowMainPromoUseCase,
            ).let(::add)

            WalletNFTListSubscriber(
                userWallet = userWallet,
                getNFTCollectionsUseCase = getNFTCollectionsUseCase,
                stateHolder = stateHolder,
                walletsRepository = walletsRepository,
                clickIntents = clickIntents,
                currenciesRepository = currenciesRepository,
            ).let(::add)

            MultiWalletWarningsSubscriber(
                userWallet = userWallet,
                stateHolder = stateHolder,
                clickIntents = clickIntents,
                getMultiWalletWarningsFactory = getMultiWalletWarningsFactory,
                walletWarningsAnalyticsSender = walletWarningsAnalyticsSender,
                walletWarningsSingleEventSender = walletWarningsSingleEventSender,
            ).let(::add)

            MultiWalletActionButtonsSubscriber(
                userWallet = userWallet,
                stateHolder = stateHolder,
                getStoryContentUseCase = getStoryContentUseCase,
            ).let(::add)

            WalletDropDownItemsSubscriber(
                stateHolder = stateHolder,
                shouldSaveUserWalletsUseCase = shouldSaveUserWalletsUseCase,
                clickIntents = clickIntents,
                hotWalletFeatureToggles = hotWalletFeatureToggles,
            ).let(::add)

            if (tangemPayFeatureToggles.isTangemPayEnabled) {
                add(tangemPayMainSubscriberFactory.create(userWallet))
            }
        }
    }
}