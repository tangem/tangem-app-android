package com.tangem.feature.wallet.presentation.wallet.subscribers

import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import arrow.core.Either
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTxHistoryCountErrorTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTxHistoryCountTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTxHistoryItemsErrorTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTxHistoryItemsTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.TxHistoryItemStateConverter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Deprecated("Remove with main toggle [DesignFeatureToggles.isRedesignEnabled]")
@Suppress("LongParameterList")
internal class TxHistorySubscriberLegacy @AssistedInject constructor(
    @Assisted override val userWallet: UserWallet.Cold,
    @Assisted private val isRefresh: Boolean,
    override val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val stateController: WalletStateController,
    private val clickIntents: WalletClickIntents,
) : BasicSingleWalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<PagingData<TxInfo>> {
        return flow {
            getPrimaryCurrencyStatusFlow().collectLatest { status ->
                val maybeTxHistoryItemCount = txHistoryItemsCountUseCase(
                    userWalletId = userWallet.walletId,
                    currency = status.currency,
                )

                setLoadingTxHistoryState(maybeTxHistoryItemCount, status)

                maybeTxHistoryItemCount.onRight { _ ->
                    val maybeTxHistoryItems = txHistoryItemsUseCase(
                        userWalletId = userWallet.walletId,
                        currency = status.currency,
                        refresh = isRefresh,
                    ).map { it.cachedIn(coroutineScope) }

                    setLoadedTxHistoryState(maybeTxHistoryItems, currency = status.currency)
                }
            }
        }
    }

    private fun setLoadingTxHistoryState(
        maybeTxHistoryItemCount: Either<TxHistoryStateError, Int>,
        status: CryptoCurrencyStatus,
    ) {
        stateController.update(
            maybeTxHistoryItemCount.fold(
                ifLeft = { error ->
                    SetTxHistoryCountErrorTransformer(
                        userWallet = userWallet,
                        error = error,
                        pendingTransactions = status.value.pendingTransactions,
                        clickIntents = clickIntents,
                        currency = status.currency,
                    )
                },
                ifRight = { txCount ->
                    SetTxHistoryCountTransformer(
                        userWalletId = userWallet.walletId,
                        transactionsCount = txCount,
                        clickIntents = clickIntents,
                    )
                },
            ),
        )
    }

    private fun setLoadedTxHistoryState(
        maybeTxHistoryItems: Either<TxHistoryListError, Flow<PagingData<TxInfo>>>,
        currency: CryptoCurrency,
    ) {
        stateController.update(
            maybeTxHistoryItems.fold(
                ifLeft = { error ->
                    SetTxHistoryItemsErrorTransformer(
                        userWalletId = userWallet.walletId,
                        error = error,
                        clickIntents = clickIntents,
                    )
                },
                ifRight = { itemsFlow ->
                    val blockchain = userWallet.scanResponse.cardTypesResolver.getBlockchain()
                    val itemConverter = TxHistoryItemStateConverter(
                        currency = currency,
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

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet.Cold, isRefresh: Boolean): TxHistorySubscriberLegacy
    }
}