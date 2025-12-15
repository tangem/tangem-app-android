package com.tangem.features.account.archived

import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
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
import com.tangem.core.ui.utils.showErrorDialog
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.status.usecase.RecoverCryptoPortfolioUseCase
import com.tangem.domain.account.usecase.GetArchivedAccountsUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.features.account.ArchivedAccountListComponent
import com.tangem.features.account.archived.entity.AccountArchivedUM
import com.tangem.features.account.archived.entity.AccountArchivedUMBuilder
import com.tangem.features.account.archived.entity.AccountArchivedUMBuilder.Companion.toggleProgress
import com.tangem.features.account.createedit.error.AccountFeatureError
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
) : Model() {

    private val params = paramsContainer.require<ArchivedAccountListComponent.Params>()
    private val onCloseClick = { router.pop() }

    val uiState: StateFlow<AccountArchivedUM>
        field = MutableStateFlow(getInitialState())

    private val getArchivedAccountsJob = JobHolder()

    init {
        getArchivedAccounts()
    }

    private fun getArchivedAccounts() {
        getArchivedAccountsUseCase(params.userWalletId)
            .conflate()
            .distinctUntilChanged()
            .onEach { lce ->
                val newState = lce.fold(
                    ifLoading = { content ->
                        content ?: return@fold null

                        umBuilder.mapContent(
                            accounts = content,
                            onCloseClick = onCloseClick,
                            onRecoverClick = { recoverCryptoPortfolio(accountId = it.accountId) },
                        )
                    },
                    ifContent = { content ->
                        umBuilder.mapContent(
                            accounts = content,
                            onCloseClick = onCloseClick,
                            onRecoverClick = { recoverCryptoPortfolio(accountId = it.accountId) },
                        )
                    },
                    ifError = { error ->
                        umBuilder.mapError(
                            throwable = error,
                            onCloseClick = onCloseClick,
                            getArchivedAccounts = { getArchivedAccounts() },
                        )
                    },
                )

                if (newState != null) {
                    uiState.value = newState
                }
            }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
            .saveIn(getArchivedAccountsJob)
    }

    private fun recoverCryptoPortfolio(accountId: AccountId) = modelScope.launch {
        uiState.update { it.toggleProgress(accountId, isLoading = true) }
        val result = withContext(dispatchers.default) {
            recoverCryptoPortfolioUseCase(accountId)
        }
        uiState.update { it.toggleProgress(accountId, isLoading = false) }
        result
            .onLeft(::handleRecoverError)
            .onRight {
                showSuccessRecoverMessage()
                router.pop()
            }
    }

    private fun handleRecoverError(error: RecoverCryptoPortfolioUseCase.Error) {
        if (error is RecoverCryptoPortfolioUseCase.Error.AccountListRequirementsNotMet &&
            error.cause is AccountList.Error.ExceedsMaxAccountsCount
        ) {
            val firstAction = EventMessageAction(
                title = resourceReference(R.string.common_got_it),
                onClick = { },
            )

            messageSender.send(
                DialogMessage(
                    title = resourceReference(R.string.account_recover_limit_dialog_title),
                    message = resourceReference(
                        id = R.string.account_recover_limit_dialog_description,
                        formatArgs = wrappedList(AccountList.MAX_ACCOUNTS_COUNT.toString()),
                    ),
                    firstActionBuilder = { firstAction },
                ),
            )

            return
        }

        val featureError = AccountFeatureError.ArchivedAccountList.FailedToRecoverAccount(cause = error)
        logError(error = featureError)
        messageSender.showErrorDialog(universalError = featureError, onDismiss = router::pop)
    }

    private fun logError(error: AccountFeatureError, params: Map<String, String> = emptyMap()) {
        val exception = IllegalStateException(error.toString())

        Timber.e(exception)

        analyticsExceptionHandler.sendException(
            event = ExceptionAnalyticsEvent(exception = exception, params = params),
        )
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