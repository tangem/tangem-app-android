package com.tangem.feature.wallet.presentation.wallet.subscribers

import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.visa.GetVisaCurrencyUseCase
import com.tangem.domain.visa.GetVisaTxHistoryUseCase
import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.domain.visa.model.VisaTxHistoryItem
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetVisaInfoTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTxHistoryCountTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTxHistoryItemsErrorTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTxHistoryItemsTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.VisaTxHistoryItemStateConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

internal class VisaWalletSubscriber(
    private val userWallet: UserWallet.Cold,
    private val stateController: WalletStateController,
    private val isRefresh: Boolean,
    private val getVisaCurrencyUseCase: GetVisaCurrencyUseCase,
    private val getVisaTxHistoryUseCase: GetVisaTxHistoryUseCase,
    private val clickIntents: WalletClickIntents,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return flow<Any> {
            setLoadingTxHistoryState()

            val maybeCurrency = getVisaCurrencyUseCase(userWallet.walletId, isRefresh)
            setLoadedCurrencyState(maybeCurrency)

            val currency = maybeCurrency.getOrElse {
                Timber.e(it, "Failed to load VISA currency")
                setFailedTxHistoryState(it)
                return@flow
            }
            val txHistoryItemsFlow = getVisaTxHistoryUseCase(userWallet.walletId, isRefresh = isRefresh)
                .map { maybeTxHistoryItems ->
                    maybeTxHistoryItems.getOrElse {
                        Timber.e(it, "Failed to load tx history for wallet ${userWallet.walletId}")
                        throw it
                    }
                }
                .catch { setFailedTxHistoryState(it) }
                .cachedIn(coroutineScope)

            setLoadedTxHistoryState(txHistoryItemsFlow, currency)
        }
    }

    private fun setLoadedCurrencyState(maybeCurrency: Either<Throwable, VisaCurrency>) {
        stateController.update(
            SetVisaInfoTransformer(
                userWallet = userWallet,
                maybeVisaCurrency = maybeCurrency,
                clickIntents = clickIntents,
            ),
        )
    }

    private fun setLoadingTxHistoryState() {
        stateController.update(
            SetTxHistoryCountTransformer(
                userWalletId = userWallet.walletId,
                transactionsCount = 10,
                clickIntents = clickIntents,
            ),
        )
    }

    private fun setFailedTxHistoryState(it: Throwable) {
        stateController.update(
            SetTxHistoryItemsErrorTransformer(
                userWalletId = userWallet.walletId,
                error = TxHistoryListError.DataError(it),
                clickIntents = clickIntents,
            ),
        )
    }

    private fun setLoadedTxHistoryState(itemsFlow: Flow<PagingData<VisaTxHistoryItem>>, currency: VisaCurrency) {
        val itemConverter = VisaTxHistoryItemStateConverter(currency, clickIntents)

        stateController.update(
            SetTxHistoryItemsTransformer(
                userWallet = userWallet,
                flow = itemsFlow.map { items ->
                    items.map(itemConverter::convert)
                },
                clickIntents = clickIntents,
            ),
        )
    }
}