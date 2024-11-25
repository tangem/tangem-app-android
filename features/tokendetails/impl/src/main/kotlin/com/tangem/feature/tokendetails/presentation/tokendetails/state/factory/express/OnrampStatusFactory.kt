package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.onramp.GetOnrampStatusUseCase
import com.tangem.domain.onramp.GetOnrampTransactionsUseCase
import com.tangem.domain.onramp.OnrampRemoveTransactionUseCase
import com.tangem.domain.onramp.OnrampSaveTransactionUseCase
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExpressTransactionStateUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsOnrampTransactionStateConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.onramp.OnrampStatusBottomSheetConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
internal class OnrampStatusFactory(
    private val stateProvider: Provider<TokenDetailsState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val clickIntents: TokenDetailsClickIntents,
    private val cryptoCurrency: CryptoCurrency,
    private val userWalletId: UserWalletId,
    private val getOnrampTransactionsUseCase: GetOnrampTransactionsUseCase,
    private val onrampSaveTransactionUseCase: OnrampSaveTransactionUseCase,
    private val getOnrampStatusUseCase: GetOnrampStatusUseCase,
    private val onrampRemoveTransactionUseCase: OnrampRemoveTransactionUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    private val onrampTransactionStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        TokenDetailsOnrampTransactionStateConverter(
            clickIntents = clickIntents,
            cryptoCurrency = cryptoCurrency,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
            appCurrencyProvider = appCurrencyProvider,
        )
    }

    operator fun invoke(): Flow<List<OnrampTransaction>> {
        return getOnrampTransactionsUseCase(
            userWalletId = userWalletId,
            cryptoCurrencyId = cryptoCurrency.id,
        ).fold(
            ifRight = { savedTransactions ->
                savedTransactions
            },
            ifLeft = { flowOf(persistentListOf()) },
        )
    }

    fun getStateWithOnrampStatusBottomSheet(onrampTxState: ExpressTransactionStateUM.OnrampUM): TokenDetailsState {
        return stateProvider().copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = clickIntents::onDismissBottomSheet,
                content = OnrampStatusBottomSheetConfig(onrampTxState),
            ),
        )
    }

    fun updateOnrampStatusBottomSheet(onrampTxs: List<OnrampTransaction>): TokenDetailsState {
        val state = stateProvider()
        val bottomSheetConfig = state.bottomSheetConfig
        val onrampBottomSheet = bottomSheetConfig?.content as? OnrampStatusBottomSheetConfig

        val updatedOnrampTxState = onrampTransactionStateConverter.convertList(onrampTxs).toPersistentList()
        val currentOnrampTxState =
            updatedOnrampTxState.firstOrNull { it.info.txId == onrampBottomSheet?.value?.info?.txId }

        return state.copy(
            onrampTxs = updatedOnrampTxState,
            bottomSheetConfig = bottomSheetConfig?.copy(
                content = if (currentOnrampTxState != null && currentOnrampTxState != onrampBottomSheet?.value) {
                    OnrampStatusBottomSheetConfig(currentOnrampTxState)
                } else {
                    onrampBottomSheet
                } ?: bottomSheetConfig.content,
            ),
        )
    }

    suspend fun removeTransactionOnBottomSheetClosed(): TokenDetailsState {
        val state = stateProvider()
        val bottomSheetConfig = state.bottomSheetConfig?.content as? OnrampStatusBottomSheetConfig ?: return state
        val selectedTx = bottomSheetConfig.value

        return if (selectedTx.activeStatus.isTerminal()) {
            onrampRemoveTransactionUseCase(txId = selectedTx.info.txId)
            val filteredTxs = state.onrampTxs
                .filterNot { it.info.txId == selectedTx.info.txId }
                .toPersistentList()
            state.copy(onrampTxs = filteredTxs)
        } else {
            state
        }
    }

    suspend fun updateOnrmapTxStatuses(onrampTxList: List<OnrampTransaction>) = withContext(dispatchers.io) {
        onrampTxList.map { tx ->
            async {
                getOnrampStatusUseCase(tx.txId).fold(
                    ifLeft = { null },
                    ifRight = { statusModel ->
                        val updatedStatus = tx.copy(status = statusModel.status)
                        onrampSaveTransactionUseCase(updatedStatus)
                        updatedStatus
                    },
                )
            }
        }
            .awaitAll()
            .filterNotNull()
            .toPersistentList()
    }
}