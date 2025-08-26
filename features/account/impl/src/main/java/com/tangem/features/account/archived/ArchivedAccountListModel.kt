package com.tangem.features.account.archived

import com.tangem.common.ui.account.toUM
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
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
import com.tangem.features.account.archived.entity.ArchivedAccountUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("UnusedPrivateMember") // todo account
internal class ArchivedAccountListModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val messageSender: UiMessageSender,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val recoverCryptoPortfolioUseCase: RecoverCryptoPortfolioUseCase,
    private val getArchivedAccountsUseCase: GetArchivedAccountsUseCase,
) : Model() {

    private val params = paramsContainer.require<ArchivedAccountListComponent.Params>()
    private val onCloseClick = { router.pop() }

    val uiState: StateFlow<AccountArchivedUM> get() = _uiState
    private val _uiState: MutableStateFlow<AccountArchivedUM> = MutableStateFlow(getInitialState())
    private var getArchivedAccountsJob: Job? = null

    init {
        getArchivedAccounts()
    }

    private fun getArchivedAccounts() {
        getArchivedAccountsJob?.cancel()
        getArchivedAccountsJob = getArchivedAccountsUseCase(params.userWalletId)
            .onEach {
                val newState = when (it) {
                    is Lce.Content<ArchivedAccountList> -> mapContent(it.content)
                    is Lce.Error<Throwable> -> mapError(it.error)
                    is Lce.Loading<ArchivedAccountList> -> it.partialContent?.let(::mapContent)
                }
                newState?.let { _uiState.value = newState }
            }
            .launchIn(modelScope)
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
                    formatArgs = wrappedList(account.name.value),
                ),
                firstActionBuilder = { firstAction },
                secondActionBuilder = { secondAction },
            ),
        )
    }

    private fun recoverCryptoPortfolio(accountId: AccountId) = modelScope.launch {
        recoverCryptoPortfolioUseCase(accountId)
            .onLeft {
                Timber.tag(it.tag).e(it.toString())
                router.pop()
            }
            .onRight {
                showSuccessRecoverMessage()
                router.pop()
            }
    }

    fun showSuccessRecoverMessage() {
        val message = resourceReference(R.string.account_recover_success_message)
        messageSender.send(ToastMessage(message = message))
    }

    private fun getInitialState(): AccountArchivedUM {
        return AccountArchivedUM.Loading(
            onCloseClick = onCloseClick,
        )
    }

    private fun mapContent(accounts: ArchivedAccountList) = AccountArchivedUM.Content(
        onCloseClick = onCloseClick,
        accounts = accounts
            .map { account -> account.mapArchivedAccountUM() }
            .toImmutableList(),
    )

    private fun ArchivedAccount.mapArchivedAccountUM() = ArchivedAccountUM(
        accountId = accountId.value,
        accountName = stringReference(name.value),
        accountIconUM = icon.toUM(),
        tokensInfo = pluralReference(
            R.plurals.common_tokens_count,
            count = tokensCount,
            formatArgs = wrappedList(tokensCount),
        ),
        networksInfo = pluralReference(
            R.plurals.common_networks_count,
            count = networksCount,
            formatArgs = wrappedList(networksCount),
        ),
        onClick = { confirmRecoverDialog(this) },
    )

    private fun mapError(throwable: Throwable): AccountArchivedUM.Error {
        Timber.e(throwable)
        return AccountArchivedUM.Error(
            onCloseClick = onCloseClick,
            onRetryClick = { getArchivedAccounts() },
        )
    }
}