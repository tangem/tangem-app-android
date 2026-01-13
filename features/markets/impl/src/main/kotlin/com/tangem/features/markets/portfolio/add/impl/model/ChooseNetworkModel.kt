package com.tangem.features.markets.portfolio.add.impl.model

import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.managetokens.CheckCurrencyUnsupportedUseCase
import com.tangem.domain.managetokens.model.CurrencyUnsupportedState
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.add.impl.ChooseNetworkComponent
import com.tangem.features.markets.portfolio.add.impl.ui.state.ChooseNetworkUM
import com.tangem.features.markets.portfolio.impl.model.BlockchainRowUMConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class ChooseNetworkModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val messageSender: UiMessageSender,
    private val checkCurrencyUnsupportedUseCase: CheckCurrencyUnsupportedUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<ChooseNetworkComponent.Params>()

    val uiState: StateFlow<ChooseNetworkUM> = MutableStateFlow(buildUI())

    private fun buildUI(): ChooseNetworkUM {
        val allAvailable = params.selectedPortfolio.account.availableNetworks
        val alreadyAdded = allAvailable
            .subtract(params.selectedPortfolio.account.availableToAddNetworks)
        val converter = BlockchainRowUMConverter(
            alreadyAddedNetworks = alreadyAdded.mapTo(mutableSetOf()) { it.networkId },
        )
        val allAvailableNetworks = allAvailable.map { it to true }
        return ChooseNetworkUM(
            networks = converter.convertList(allAvailableNetworks).toPersistentList(),
            onNetworkClick = onNetworkClick@{ row ->
                val network = allAvailable
                    .find { it.networkId == row.id }
                    ?: return@onNetworkClick
                checkNetwork(row, network)
            },
        )
    }

    private fun checkNetwork(row: BlockchainRowUM, network: TokenMarketInfo.Network) = modelScope.launch {
        val selectedWalletId = params.selectedPortfolio.userWallet.walletId
        val unsupportedState = checkCurrencyUnsupportedState(
            userWalletId = selectedWalletId,
            rawNetworkId = row.id,
            isMainNetwork = row.isMainNetwork,
        )
        if (unsupportedState != null) {
            showUnsupportedWarning(unsupportedState)
        } else {
            params.callbacks.onNetworkSelected(network)
        }
    }

    private suspend fun checkCurrencyUnsupportedState(
        userWalletId: UserWalletId,
        rawNetworkId: String,
        isMainNetwork: Boolean,
    ): CurrencyUnsupportedState? {
        return checkCurrencyUnsupportedUseCase(
            userWalletId = userWalletId,
            networkId = rawNetworkId,
            isMainNetwork = isMainNetwork,
        ).getOrElse { error ->
            Timber.e(
                error,
                """
                    Failed to check currency unsupported state
                    |- User wallet ID: $userWalletId
                    |- Network ID: $rawNetworkId
                    |- Is main network: $isMainNetwork
                """.trimIndent(),
            )

            val message = SnackbarMessage(
                message = error.localizedMessage?.let(::stringReference) ?: resourceReference(R.string.common_error),
            )
            messageSender.send(message)

            null
        }
    }

    private fun showUnsupportedWarning(unsupportedState: CurrencyUnsupportedState) {
        val message = DialogMessage(
            message = when (unsupportedState) {
                is CurrencyUnsupportedState.Token.NetworkTokensUnsupported -> resourceReference(
                    id = R.string.alert_manage_tokens_unsupported_message,
                    formatArgs = wrappedList(unsupportedState.networkName),
                )
                is CurrencyUnsupportedState.Token.UnsupportedCurve -> resourceReference(
                    id = R.string.alert_manage_tokens_unsupported_curve_message,
                    formatArgs = wrappedList(unsupportedState.networkName),
                )
                is CurrencyUnsupportedState.UnsupportedNetwork -> resourceReference(
                    id = R.string.alert_manage_tokens_unsupported_curve_message,
                    formatArgs = wrappedList(unsupportedState.networkName),
                )
            },
        )

        messageSender.send(message)
    }
}