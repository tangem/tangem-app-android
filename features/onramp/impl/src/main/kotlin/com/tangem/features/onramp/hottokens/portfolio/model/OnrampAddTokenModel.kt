package com.tangem.features.onramp.hottokens.portfolio.model

import com.tangem.common.ui.addtoken.AddTokenUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.BackendId
import com.tangem.domain.wallets.usecase.ColdWalletAndHasMissedDerivationsUseCase
import com.tangem.features.onramp.hottokens.portfolio.OnrampAddTokenComponent
import com.tangem.features.onramp.hottokens.portfolio.OnrampAddTokenComponent.AddHotCryptoData
import com.tangem.features.onramp.hottokens.portfolio.entity.OnrampAddTokenUiBuilder
import com.tangem.features.onramp.hottokens.portfolio.entity.OnrampAddTokenUiBuilder.Companion.toggleProgress
import com.tangem.features.onramp.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class OnrampAddTokenModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val uiBuilder: OnrampAddTokenUiBuilder,
    private val messageSender: UiMessageSender,
    private val coldWalletAndHasMissedDerivationsUseCase: ColdWalletAndHasMissedDerivationsUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
) : Model() {

    private val params = paramsContainer.require<OnrampAddTokenComponent.Params>()
    private val addTokenJob = JobHolder()

    val uiState: StateFlow<AddTokenUM?>
        field = MutableStateFlow(value = null)

    init {
        params.tokenToAdd
            .distinctUntilChanged()
            .mapLatest { tokenToAdd: AddHotCryptoData ->
                addTokenJob.cancel()
                val backendId = tokenToAdd.cryptoCurrency.network.backendId
                val userWalletId = tokenToAdd.account.accountId.userWalletId
                val isTangemIconVisible = needColdWalletInteraction(userWalletId, backendId)
                uiBuilder.updateContent(
                    tokenToAdd = tokenToAdd,
                    isTangemIconVisible = isTangemIconVisible,
                    onConfirmClick = { onAddClick(tokenToAdd).saveIn(addTokenJob) },
                )
            }
            .onEach { newUI -> uiState.value = newUI }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun onAddClick(tokenToAdd: AddHotCryptoData) = modelScope.launch(dispatchers.default) {
        val um = uiState.value ?: return@launch
        uiState.value = um.toggleProgress(true)

        val cryptoCurrency = tokenToAdd.cryptoCurrency
        val accountId = tokenToAdd.account.accountId
        manageCryptoCurrenciesUseCase(accountId = accountId, add = cryptoCurrency)
            .onLeft { throwable ->
                processError(error = throwable)
                uiState.value = um.toggleProgress(false)
                return@launch
            }

        val status = getAccountCurrencyStatusUseCase(
            userWalletId = accountId.userWalletId,
            currencyId = cryptoCurrency.id,
            network = cryptoCurrency.network,
        ).firstOrNull()
        if (status == null) {
            processError(error = null)
        } else {
            params.callbacks.onTokenAdded(status.status)
        }
        uiState.value = um.toggleProgress(false)
    }

    private suspend fun needColdWalletInteraction(walletId: UserWalletId, backendId: BackendId): Boolean =
        coldWalletAndHasMissedDerivationsUseCase.invoke(
            userWalletId = walletId,
            networksWithDerivationPath = mapOf(backendId to null),
        )

    private fun processError(error: Throwable?) {
        val message = error?.message?.let { stringReference(it) }
            ?: resourceReference(R.string.common_something_went_wrong)
        messageSender.send(ToastMessage(message = message))
    }
}