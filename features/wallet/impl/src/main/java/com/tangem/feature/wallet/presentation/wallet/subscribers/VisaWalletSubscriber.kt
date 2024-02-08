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
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetBalancesAndLimitsTransformer
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetTxHistoryCountTransformer
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetTxHistoryItemsErrorTransformer
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetTxHistoryItemsTransformer
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter.VisaTxHistoryItemStateConverter
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

internal class VisaWalletSubscriber(
    private val userWallet: UserWallet,
    private val stateController: WalletStateController,
    private val isRefresh: Boolean,
    private val getVisaCurrencyUseCase: GetVisaCurrencyUseCase,
    private val getVisaTxHistoryUseCase: GetVisaTxHistoryUseCase,
    private val clickIntents: WalletClickIntentsV2,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return flow<Any> {
            setLoadingTxHistoryState()

            val maybeCurrency = getVisaCurrencyUseCase(userWallet.walletId, isRefresh)
            setLoadedCurrencyState(maybeCurrency)

            val currency = maybeCurrency.getOrElse {
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
            SetBalancesAndLimitsTransformer(
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