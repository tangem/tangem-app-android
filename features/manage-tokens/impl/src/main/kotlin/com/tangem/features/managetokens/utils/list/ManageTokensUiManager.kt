package com.tangem.features.managetokens.utils.list

import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.features.managetokens.component.ManageTokensSource
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.utils.mapper.toUiModel
import com.tangem.features.managetokens.utils.ui.update
import com.tangem.pagination.Batch
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class ManageTokensUiManager(
    private val state: MutableStateFlow<ManageTokensListState>,
    private val messageSender: UiMessageSender,
    private val dispatchers: CoroutineDispatcherProvider,
    private val scopeProvider: Provider<CoroutineScope>,
    private val sourceProvider: Provider<ManageTokensSource>,
    private val actions: ManageTokensUiActions,
) {

    private val scope: CoroutineScope
        get() = scopeProvider()

    private val source: ManageTokensSource
        get() = sourceProvider()

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: Flow<ImmutableList<CurrencyItemUM>> = state
        .mapLatest { state ->
            state.uiBatches.asSequence()
                .flatMap { it.data }
                .toImmutableList()
        }
        .distinctUntilChanged()

    fun createOrUpdateUiBatches(
        newCurrencyBatches: List<Batch<Int, List<ManagedCryptoCurrency>>>,
        canEditItems: Boolean,
    ): List<Batch<Int, List<CurrencyItemUM>>> {
        val currentUiBatches = state.value.uiBatches
        val batches = currentUiBatches.toMutableList()

        newCurrencyBatches.forEach { (key, data) ->
            val indexToUpdate = currentUiBatches.indexOfFirst { it.key == key }
            val currencyBatch = state.value.currencyBatches.getOrNull(indexToUpdate)

            if (indexToUpdate == -1 || currencyBatch == null) {
                val newBatch = Batch(
                    key = key,
                    data = data.map { item ->
                        item.toUiModel(
                            isEditable = canEditItems,
                            onRemoveCustomCurrencyClick = ::removeCustomCurrency,
                            onTokenClick = actions::onTokenClick,
                        )
                    },
                )

                batches.addOrReplace(newBatch) { it.key == key }
            } else {
                val uiBatchToUpdate = currentUiBatches[indexToUpdate]

                if (uiBatchToUpdate.data == data) {
                    return@forEach
                }

                val updatedBatch = uiBatchToUpdate.copy(
                    data = data.mapIndexed { index, item ->
                        if (item == currencyBatch.data[index]) {
                            return@mapIndexed uiBatchToUpdate.data[index]
                        }

                        val previousUiItem = uiBatchToUpdate.data.getOrNull(index)
                        if (previousUiItem == null || previousUiItem.id != item.id) {
                            item.toUiModel(
                                isEditable = canEditItems,
                                onRemoveCustomCurrencyClick = ::removeCustomCurrency,
                                onTokenClick = actions::onTokenClick,
                            )
                        } else {
                            previousUiItem.update(item)
                        }
                    },
                )

                batches[indexToUpdate] = updatedBatch
            }
        }

        return batches
    }

    private fun removeCustomCurrency(currency: ManagedCryptoCurrency.Custom) = scope.launch(dispatchers.default) {
        showRemoveNetworkWarning(
            currency = currency,
            network = currency.network,
            isCoin = currency is ManagedCryptoCurrency.Custom.Coin,
            onConfirm = {
                val userWalletId = requireNotNull(state.value.userWalletId) { "UserWalletId is null. Can not remove" }
                actions.removeCustomCurrency(userWalletId = userWalletId, currency = currency)
            },
        )
    }

    private suspend fun showRemoveNetworkWarning(
        currency: ManagedCryptoCurrency,
        network: Network,
        isCoin: Boolean,
        onConfirm: () -> Unit,
    ) {
        val userWalletId = state.value.userWalletId
        val hasLinkedTokens = if (userWalletId == null || !isCoin) {
            false
        } else {
            actions.checkHasLinkedTokens(userWalletId, network)
        }
        val canHideWithoutConfirming = source == ManageTokensSource.ONBOARDING

        if (hasLinkedTokens) {
            showLinkedTokensWarning(currency, network)
        } else if (canHideWithoutConfirming) {
            onConfirm()
        } else {
            showHideTokenWarning(currency, onConfirm)
        }
    }

    private fun showLinkedTokensWarning(currency: ManagedCryptoCurrency, network: Network) {
        val message = DialogMessage(
            title = resourceReference(
                id = R.string.token_details_unable_hide_alert_title,
                formatArgs = wrappedList(currency.name),
            ),
            message = resourceReference(
                id = R.string.token_details_unable_hide_alert_message,
                formatArgs = wrappedList(
                    currency.name,
                    currency.symbol,
                    network.name,
                ),
            ),
        )
        messageSender.send(message)
    }

    private fun showHideTokenWarning(currency: ManagedCryptoCurrency, onConfirm: () -> Unit) {
        val message = DialogMessage(
            title = resourceReference(
                id = R.string.token_details_hide_alert_title,
                formatArgs = wrappedList(currency.name),
            ),
            message = resourceReference(R.string.token_details_hide_alert_message),
            firstActionBuilder = {
                EventMessageAction(
                    title = resourceReference(R.string.token_details_hide_alert_hide),
                    warning = true,
                    onClick = onConfirm,
                )
            },
            secondActionBuilder = { cancelAction() },
        )

        messageSender.send(message)
    }
}