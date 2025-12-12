package com.tangem.feature.wallet.presentation.wallet.subscribers

import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTxHistoryCountErrorTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTxHistoryCountTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTxHistoryItemsErrorTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTxHistoryItemsTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.TxHistoryItemStateConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Suppress("LongParameterList")
internal class TxHistorySubscriberV2(
    override val userWallet: UserWallet.Cold,
    override val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val isRefresh: Boolean,
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

                maybeTxHistoryItemCount.onRight {
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

    private fun setLoadingTxHistoryState(maybeTxHistoryItemCount: MaybeTxHistoryCount, status: CryptoCurrencyStatus) {
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

    private fun setLoadedTxHistoryState(maybeTxHistoryItems: MaybeTxHistoryItems, currency: CryptoCurrency) {
        stateController.update(
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
}