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
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.account.producer.SingleAccountProducer
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.account.usecase.ArchiveCryptoPortfolioUseCase
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.account.AccountDetailsComponent
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder.Companion.portfolioIcon
import com.tangem.features.account.details.entity.AccountDetailsUM
import com.tangem.features.account.details.entity.AccountDetailsUM.ArchiveMode
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class AccountDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val messageSender: UiMessageSender,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val archiveCryptoPortfolioUseCase: ArchiveCryptoPortfolioUseCase,
    singleAccountSupplier: SingleAccountSupplier,
    private val getUserWalletUseCase: GetUserWalletUseCase,
) : Model() {

    private val params = paramsContainer.require<AccountDetailsComponent.Params>()

    val uiState: StateFlow<AccountDetailsUM> get() = _uiState
    private val _uiState: MutableStateFlow<AccountDetailsUM> = MutableStateFlow(buildUI(params.account))
    private val accountId = params.account.accountId

    init {
        singleAccountSupplier(SingleAccountProducer.Params(accountId))
            .onEach { account -> _uiState.update { buildUI(account) } }
            .launchIn(modelScope)
    }

    private fun onEditAccountClick(account: Account) {
        router.push(AppRoute.EditAccount(account))
    }

    private fun onManageTokensClick(account: Account) {
        val route = AppRoute.ManageTokens(
            source = AppRoute.ManageTokens.Source.SETTINGS,
            portfolioId = PortfolioId(account.accountId),
        )
        router.push(route)
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
            isWarning = true,
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
        _uiState.update { it.toggleProgress(true) }
        archiveCryptoPortfolioUseCase(accountId)
            .onLeft { error ->
                failedArchiveDialog(error)
                _uiState.update { it.toggleProgress(false) }
            }
            .onRight {
                val message = resourceReference(R.string.account_archive_success_message)
                messageSender.send(ToastMessage(message = message))
                router.pop()
            }
    }

    private fun failedArchiveDialog(error: ArchiveCryptoPortfolioUseCase.Error) {
        // todo account referral case
        val titleRes = when (error) {
            is ArchiveCryptoPortfolioUseCase.Error.CriticalTechError.AccountListRequirementsNotMet,
            is ArchiveCryptoPortfolioUseCase.Error.CriticalTechError.AccountNotFound,
            is ArchiveCryptoPortfolioUseCase.Error.CriticalTechError.AccountsNotCreated,
            is ArchiveCryptoPortfolioUseCase.Error.DataOperationFailed,
            -> R.string.common_something_went_wrong
        }
        val messageRes = when (error) {
            is ArchiveCryptoPortfolioUseCase.Error.CriticalTechError.AccountListRequirementsNotMet,
            is ArchiveCryptoPortfolioUseCase.Error.CriticalTechError.AccountNotFound,
            is ArchiveCryptoPortfolioUseCase.Error.CriticalTechError.AccountsNotCreated,
            is ArchiveCryptoPortfolioUseCase.Error.DataOperationFailed,
            -> R.string.account_could_not_archive
        }

        val dialogMessage = DialogMessage(
            title = resourceReference(titleRes),
            message = resourceReference(messageRes),
        )
        messageSender.send(dialogMessage)
    }

    private fun buildUI(account: Account): AccountDetailsUM {
        val archiveMode = when (account) {
            is Account.CryptoPortfolio -> when (account.isMainAccount) {
                true -> ArchiveMode.None
                false -> ArchiveMode.Available(
                    onArchiveAccountClick = ::onArchiveAccountClick,
                    isLoading = false,
                )
            }
        }
        val isMultiCurrency = getUserWalletUseCase(account.accountId.userWalletId)
            .getOrNull()?.isMultiCurrency ?: false
        return AccountDetailsUM(
            accountName = account.accountName.toUM().value,
            accountIcon = account.portfolioIcon.toUM(),
            onCloseClick = { router.pop() },
            onAccountEditClick = { onEditAccountClick(account) },
            onManageTokensClick = { onManageTokensClick(account) },
            archiveMode = archiveMode,
            isManageTokensAvailable = isMultiCurrency,
        )
    }

    private fun AccountDetailsUM.toggleProgress(isLoading: Boolean): AccountDetailsUM {
        val archiveMode = this.archiveMode as? ArchiveMode.Available ?: return this
        return this.copy(archiveMode = archiveMode.copy(isLoading = isLoading))
    }
}