package com.tangem.features.account.details

import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.account.toUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.account.usecase.ArchiveCryptoPortfolioUseCase
import com.tangem.features.account.AccountDetailsComponent
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder.Companion.portfolioIcon
import com.tangem.features.account.details.entity.AccountDetailsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class AccountDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val messageSender: UiMessageSender,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val archiveCryptoPortfolioUseCase: ArchiveCryptoPortfolioUseCase,
) : Model() {

    private val params = paramsContainer.require<AccountDetailsComponent.Params>()

    val uiState: StateFlow<AccountDetailsUM> get() = _uiState
    private val _uiState: MutableStateFlow<AccountDetailsUM> = MutableStateFlow(getInitialState())

    private fun onEditAccountClick() {
        router.push(AppRoute.EditAccount(params.account))
    }

    private fun onManageTokensClick() {
        // todo account add account param
        router.push(AppRoute.ManageTokens(source = AppRoute.ManageTokens.Source.SETTINGS))
    }

    private fun onArchiveAccountClick() {
        confirmArchiveDialog()
    }

    private fun confirmArchiveDialog() {
        val secondAction = EventMessageAction(
            title = resourceReference(R.string.common_cancel),
            onClick = {},
        )
        val firstAction = EventMessageAction(
            title = resourceReference(R.string.account_details_archive_action),
            warning = true,
            onClick = ::archiveCryptoPortfolio,
        )
        messageSender.send(
            DialogMessage(
                title = resourceReference(R.string.account_details_archive),
                message = resourceReference(R.string.account_details_archive_description),
                firstActionBuilder = { firstAction },
                secondActionBuilder = { secondAction },
            ),
        )
    }

    private fun archiveCryptoPortfolio() = modelScope.launch {
        archiveCryptoPortfolioUseCase(params.account.accountId)
    }

    private fun getInitialState(): AccountDetailsUM {
        return AccountDetailsUM(
            accountName = params.account.accountName.value,
            accountIcon = params.account.portfolioIcon.toUM(),
            onCloseClick = { router.pop() },
            onAccountEditClick = ::onEditAccountClick,
            onManageTokensClick = ::onManageTokensClick,
            onArchiveAccountClick = ::onArchiveAccountClick,
        )
    }
}