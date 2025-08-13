package com.tangem.features.account.archived

import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.account.usecase.RecoverCryptoPortfolioUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.features.account.ArchivedAccountListComponent
import com.tangem.features.account.archived.entity.AccountArchivedUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("UnusedPrivateMember") // todo account
internal class ArchivedAccountListModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val messageSender: UiMessageSender,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val recoverCryptoPortfolioUseCase: RecoverCryptoPortfolioUseCase,
) : Model() {

    private val params = paramsContainer.require<ArchivedAccountListComponent.Params>()

    val uiState: StateFlow<AccountArchivedUM> get() = _uiState
    private val _uiState: MutableStateFlow<AccountArchivedUM> = MutableStateFlow(getInitialState())

    private fun confirmRecoverDialog(accountId: AccountId) {
        val account: Account? = null // todo account find
        account ?: return
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
                title = stringReference(account.name.value),
                message = TextReference.EMPTY,
                firstActionBuilder = { firstAction },
                secondActionBuilder = { secondAction },
            ),
        )
    }

    private fun recoverCryptoPortfolio(accountId: AccountId) = modelScope.launch {
        recoverCryptoPortfolioUseCase(accountId)
    }

    private fun getInitialState(): AccountArchivedUM {
        return AccountArchivedUM.Loading(
            onCloseClick = { router.pop() },
        )
    }
}