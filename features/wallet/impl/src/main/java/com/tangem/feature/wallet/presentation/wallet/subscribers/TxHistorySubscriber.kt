package com.tangem.feature.wallet.presentation.wallet.subscribers

import androidx.paging.PagingData
import androidx.paging.cachedIn
import arrow.core.Either
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.tokens.GetPrimaryCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.collectLatest
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.*
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

typealias MaybeTxHistoryCount = Either<TxHistoryStateError, Int>
typealias MaybeTxHistoryItems = Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>

@Suppress("LongParameterList")
internal class TxHistorySubscriber(
    private val userWallet: UserWallet,
    private val isRefresh: Boolean,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntentsV2,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<PagingData<TxHistoryItem>> {
        // TODO: [REDACTED_JIRA]
        if (userWallet.scanResponse.cardTypesResolver.isVisaWallet()) {
            return flow {
                stateHolder.update(
                    object : WalletStateTransformer(userWallet.walletId) {
                        override fun transform(prevState: WalletState): WalletState {
                            return when (prevState) {
                                is WalletState.Visa.Content -> prevState.copy(
                                    txHistoryState = TxHistoryState.Empty(onExploreClick = {}),
                                )
                                else -> prevState
                            }
                        }
                    },
                )
            }
        }

        return flow {
            getPrimaryCurrencyStatusUpdatesUseCase.collectLatest(userWalletId = userWallet.walletId) { status ->
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
                ifRight = {
                    SetTxHistoryItemsTransformer(
                        userWallet = userWallet,
                        flow = it,
                        clickIntents = clickIntents,
                    )
                },
            ),
        )
    }
}