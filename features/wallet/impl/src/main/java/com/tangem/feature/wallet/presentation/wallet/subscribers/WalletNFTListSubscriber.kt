package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.nft.GetNFTCollectionsUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.RemoveNFTCollectionsTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetNFTCollectionsTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

internal class WalletNFTListSubscriber(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val walletsRepository: WalletsRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val getNFTCollectionsUseCase: GetNFTCollectionsUseCase,
    private val clickIntents: WalletClickIntents,
) : WalletSubscriber() {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun create(coroutineScope: CoroutineScope): Flow<*> = combine(
        walletsRepository.nftEnabledStatus(userWallet.walletId),
        currenciesRepository.getWalletCurrenciesUpdates(userWallet.walletId),
    ) { nftEnabled, currencies -> nftEnabled to currencies }
        .distinctUntilChanged()
        .flatMapLatest { (nftEnabled, currencies) ->
            // if NFT is enabled for this wallet and there are currencies,
            // then start observing changes from store and apply transformer if need
            if (nftEnabled && currencies.isNotEmpty()) {
                getNFTCollectionsUseCase(userWallet.walletId)
                    .shareIn(
                        scope = coroutineScope,
                        started = SharingStarted.WhileSubscribed(),
                        replay = 1,
                    )
                    .onEach {
                        stateHolder.update(
                            SetNFTCollectionsTransformer(
                                userWalletId = userWallet.walletId,
                                nftCollections = it,
                                onItemClick = { clickIntents.onNFTClick(userWallet) },
                            ),
                        )
                    }
            } else {
                // otherwise, hide NFT from wallet
                stateHolder.update(
                    RemoveNFTCollectionsTransformer(userWallet.walletId),
                )
                emptyFlow()
            }
        }
}