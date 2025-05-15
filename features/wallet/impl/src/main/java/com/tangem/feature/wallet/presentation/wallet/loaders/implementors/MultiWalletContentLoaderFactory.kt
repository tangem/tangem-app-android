package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.common.routing.RoutingFeatureToggle
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.deeplink.DeepLinksRegistry
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.nft.GetNFTCollectionsUseCase
import com.tangem.domain.promo.GetStoryContentUseCase
import com.tangem.domain.tokens.ApplyTokenListSortingUseCase
import com.tangem.domain.tokens.RunPolkadotAccountHealthCheckUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsSingleEventSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.MultiWalletTokenListStore
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.features.nft.NFTFeatureToggles
import javax.inject.Inject

@Suppress("LongParameterList")
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
    private val runPolkadotAccountHealthCheckUseCase: RunPolkadotAccountHealthCheckUseCase,
    private val shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val getStoryContentUseCase: GetStoryContentUseCase,
    private val deepLinksRegistry: DeepLinksRegistry,
    private val nftFeatureToggles: NFTFeatureToggles,
    private val walletsRepository: WalletsRepository,
    private val getNFTCollectionsUseCase: GetNFTCollectionsUseCase,
    private val routingFeatureToggle: RoutingFeatureToggle,
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
            runPolkadotAccountHealthCheckUseCase = runPolkadotAccountHealthCheckUseCase,
            getStoryContentUseCase = getStoryContentUseCase,
            shouldSaveUserWalletsUseCase = shouldSaveUserWalletsUseCase,
            deepLinksRegistry = deepLinksRegistry,
            nftFeatureToggles = nftFeatureToggles,
            walletsRepository = walletsRepository,
            getNFTCollectionsUseCase = getNFTCollectionsUseCase,
            routingFeatureToggle = routingFeatureToggle,
        )
    }
}