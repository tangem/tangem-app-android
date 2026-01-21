package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.nft.GetNFTCollectionsUseCase
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.feature.wallet.child.wallet.model.ModelScopeDependencies
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.account.AccountsSharedFlowHolder
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.RemoveNFTCollectionsTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetNFTCollectionsTransformer
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

internal class WalletNFTListSubscriberV2 @AssistedInject constructor(
    @Assisted override val userWallet: UserWallet,
    @Assisted val modelScopeDependencies: ModelScopeDependencies,
    override val accountsSharedFlowHolder: AccountsSharedFlowHolder = modelScopeDependencies.accountsSharedFlowHolder,
    private val walletsRepository: WalletsRepository,
    private val getNFTCollectionsUseCase: GetNFTCollectionsUseCase,
    private val stateController: WalletStateController,
    private val clickIntents: WalletClickIntents,
) : BasicWalletSubscriber() {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun create(coroutineScope: CoroutineScope): Flow<*> = combine(
        flow = walletsRepository.nftEnabledStatus(userWallet.walletId),
        flow2 = getCryptoCurrencyStatusesFlow(),
        transform = ::Pair,
    )
        .distinctUntilChanged()
        .flatMapLatest { (nftEnabled, currencies) ->
            // if NFT is enabled for this wallet and there are currencies,
            // then start observing changes from store and apply transformer if need
            if (nftEnabled && currencies.isNotEmpty()) {
                getNFTCollectionsUseCase.invokeForAccounts(userWallet.walletId)
                    .shareIn(
                        scope = coroutineScope,
                        started = SharingStarted.WhileSubscribed(),
                        replay = 1,
                    )
                    .onEach { walletNFTCollections ->
                        stateController.update(
                            SetNFTCollectionsTransformer(
                                userWalletId = userWallet.walletId,
                                nftCollections = walletNFTCollections.flattenCollections,
                                onItemClick = { clickIntents.onNFTClick(userWallet) },
                            ),
                        )
                    }
            } else {
                // otherwise, hide NFT from wallet
                stateController.update(
                    RemoveNFTCollectionsTransformer(userWallet.walletId),
                )
                emptyFlow()
            }
        }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet, modelScopeDependencies: ModelScopeDependencies): WalletNFTListSubscriberV2
    }
}