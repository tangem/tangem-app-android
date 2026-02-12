package com.tangem.features.nft.mainEntry.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.nft.GetNFTCollectionsUseCase
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.features.nft.component.NFTEntryBlockComponent
import com.tangem.features.nft.entity.NFTBlockUM
import com.tangem.features.nft.mainEntry.SetNFTCollectionsTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import com.tangem.utils.transformer.update as updateTransformer

@ModelScoped
internal class NFTEntryBlockModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val nftRepository: NFTRepository,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val getNFTCollectionsUseCase: GetNFTCollectionsUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<NFTEntryBlockComponent.Params>()

    val uiState: StateFlow<NFTBlockUM>
        field = MutableStateFlow<NFTBlockUM>(NFTBlockUM.Hidden)

    init {
        params.selectedWallet.map { userWalletId ->
            combine(
                flow = nftRepository.nftEnabledStatus(userWalletId),
                flow2 = singleAccountStatusListSupplier(userWalletId).conflate().map { it.flattenCurrencies() },
                transform = ::Pair,
            ).distinctUntilChanged()
                .map { (nftEnabled, currencies) ->
                    // if NFT is enabled for this wallet and there are currencies,
                    // then start observing changes from store and apply transformer if need
                    if (nftEnabled && currencies.isNotEmpty()) {
                        getNFTCollectionsUseCase.invokeForAccounts(userWalletId)
                            .shareIn(
                                scope = modelScope,
                                started = SharingStarted.WhileSubscribed(),
                                replay = 1,
                            )
                            .onEach { walletNFTCollections ->
                                uiState.updateTransformer(
                                    SetNFTCollectionsTransformer(
                                        nftCollections = walletNFTCollections.flattenCollections,
                                        onItemClick = { /*clickIntents.onNFTClick(userWallet)*/ },
                                    )
                                )
                            }
                            .launchIn(modelScope)
                    } else {
                        // otherwise, hide NFT from wallet
                        uiState.update { NFTBlockUM.Hidden }
                    }
                }.launchIn(modelScope)
        }.launchIn(modelScope)
    }
}