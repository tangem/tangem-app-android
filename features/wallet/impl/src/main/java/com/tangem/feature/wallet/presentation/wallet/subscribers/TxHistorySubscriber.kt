package com.tangem.feature.wallet.presentation.wallet.subscribers

import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import arrow.core.Either
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.domain.collectLatest
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTxHistoryCountErrorTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTxHistoryCountTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTxHistoryItemsErrorTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTxHistoryItemsTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.TxHistoryItemStateConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

typealias MaybeTxHistoryCount = Either<TxHistoryStateError, Int>
typealias MaybeTxHistoryItems = Either<TxHistoryListError, Flow<PagingData<TxInfo>>>

@Suppress("LongParameterList")
internal class TxHistorySubscriber(
    private val userWallet: UserWallet.Cold,
    private val isRefresh: Boolean,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<PagingData<TxInfo>> {
        return flow {
            getSingleCryptoCurrencyStatusUseCase.collectLatest(userWalletId = userWallet.walletId) { status ->
                val maybeTxHistoryItemCount = txHistoryItemsCountUseCase(
                    userWalletId = userWallet.walletId,
                    currency = status.currency,
                )

                setLoadingTxHistoryState(maybeTxHistoryItemCount, status)

                maybeTxHistoryItemCount.onRight {
                    val maybeTxHistoryItems = txHistoryItemsUseCase(
                        userWalletId = userWallet.walletId,
                        currency = status.currency,
                        refresh = isRefresh,
                    ).map { it.cachedIn(coroutineScope) }

                    setLoadedTxHistoryState(maybeTxHistoryItems)
                }
            }
        }
    }

    private fun setLoadingTxHistoryState(maybeTxHistoryItemCount: MaybeTxHistoryCount, status: CryptoCurrencyStatus) {
        stateHolder.update(
            maybeTxHistoryItemCount.fold(
                ifLeft = {
                    SetTxHistoryCountErrorTransformer(
                        userWallet = userWallet,
                        error = it,
                        pendingTransactions = status.value.pendingTransactions,
                        clickIntents = clickIntents,
                    )
                },
                ifRight = {
                    SetTxHistoryCountTransformer(
                        userWalletId = userWallet.walletId,
                        transactionsCount = it,
                        clickIntents = clickIntents,
                    )
                },
            ),
        )
    }

    private fun setLoadedTxHistoryState(maybeTxHistoryItems: MaybeTxHistoryItems) {
        stateHolder.update(
            maybeTxHistoryItems.fold(
                ifLeft = {
                    SetTxHistoryItemsErrorTransformer(
                        userWalletId = userWallet.walletId,
                        error = it,
                        clickIntents = clickIntents,
                    )
                },
                ifRight = { itemsFlow ->
                    val blockchain = userWallet.scanResponse.cardTypesResolver.getBlockchain()
                    val itemConverter = TxHistoryItemStateConverter(
                        symbol = blockchain.currency,
                        decimals = blockchain.decimals(),
                        clickIntents = clickIntents,
                    )

                    SetTxHistoryItemsTransformer(
                        userWallet = userWallet,
                        flow = itemsFlow.map { items ->
                            items.map(itemConverter::convert)
                        },
                        clickIntents = clickIntents,
                    )
                },
            ),
        )
    }
}