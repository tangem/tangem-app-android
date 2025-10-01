package com.tangem.features.account.archived

import com.tangem.common.ui.account.toUM
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.account.usecase.ArchivedAccountList
import com.tangem.domain.account.usecase.GetArchivedAccountsUseCase
import com.tangem.domain.account.usecase.RecoverCryptoPortfolioUseCase
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.account.AccountId
import com.tangem.features.account.ArchivedAccountListComponent
import com.tangem.features.account.archived.entity.AccountArchivedUM
import com.tangem.features.account.archived.entity.AccountArchivedUMBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
internal class ArchivedAccountListModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val messageSender: UiMessageSender,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val recoverCryptoPortfolioUseCase: RecoverCryptoPortfolioUseCase,
    private val getArchivedAccountsUseCase: GetArchivedAccountsUseCase,
    private val umBuilder: AccountArchivedUMBuilder,
) : Model() {

    private val params = paramsContainer.require<ArchivedAccountListComponent.Params>()
    private val onCloseClick = { router.pop() }

    val uiState: StateFlow<AccountArchivedUM> get() = _uiState
    private val _uiState: MutableStateFlow<AccountArchivedUM> = MutableStateFlow(getInitialState())
    private var getArchivedAccountsJob = JobHolder()

    init {
        getArchivedAccounts()
    }

    private fun getArchivedAccounts() {
        getArchivedAccountsUseCase(params.userWalletId)
            .conflate()
            .distinctUntilChanged()
            .onEach { lce ->
                val newState = when (lce) {
                    is Lce.Content<ArchivedAccountList> -> umBuilder.mapContent(
                        accounts = lce.content,
                        onCloseClick = onCloseClick,
                        confirmRecoverDialog = { confirmRecoverDialog(it) },
                    )
                    is Lce.Error<Throwable> -> umBuilder.mapError(
                        throwable = lce.error,
                        onCloseClick = onCloseClick,
                        getArchivedAccounts = { getArchivedAccounts() },
                    )
                    is Lce.Loading<ArchivedAccountList> -> lce.partialContent?.let { content ->
                        umBuilder.mapContent(
                            accounts = content,
                            onCloseClick = onCloseClick,
                            confirmRecoverDialog = { confirmRecoverDialog(it) },
                        )
                    }
                }
                newState?.let { _uiState.value = newState }
            }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
            .saveIn(getArchivedAccountsJob)
    }

    private fun confirmRecoverDialog(account: ArchivedAccount) {
        val secondAction = EventMessageAction(
            title = resourceReference(R.string.common_cancel),
            onClick = {},
        )
        val firstAction = EventMessageAction(
            title = resourceReference(R.string.account_archived_recover),
            onClick = { recoverCryptoPortfolio(account.accountId) },
        )
        messageSender.send(
            DialogMessage(
                title = resourceReference(R.string.account_archived_recover_dialog_title),
                message = resourceReference(
                    id = R.string.account_archived_recover_dialog_description,
                    formatArgs = wrappedList(account.name.toUM().value),
                ),
                firstActionBuilder = { firstAction },
                secondActionBuilder = { secondAction },
            ),
        )
    }

    private fun recoverCryptoPortfolio(accountId: AccountId) = modelScope.launch {
        recoverCryptoPortfolioUseCase(accountId)
            .onLeft { Timber.e(it.toString()) }
            .onRight { showSuccessRecoverMessage() }
        router.pop()
    }

    private fun showSuccessRecoverMessage() {
        val message = resourceReference(R.string.account_recover_success_message)
        messageSender.send(ToastMessage(message = message))
    }

    private fun getInitialState(): AccountArchivedUM {
        return AccountArchivedUM.Loading(
            onCloseClick = onCloseClick,
        )
    }
}