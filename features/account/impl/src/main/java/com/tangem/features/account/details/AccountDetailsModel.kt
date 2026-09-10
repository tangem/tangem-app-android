package com.tangem.features.account.details

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.account.status.producer.SingleAccountStatusProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusSupplier
import com.tangem.domain.account.status.usecase.ArchiveCryptoPortfolioUseCase
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.account.derivationIndex
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.account.AccountDetailsComponent
import com.tangem.features.account.analytics.AccountSettingsAnalyticEvents
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
    singleAccountSupplier: SingleAccountSupplier,
    getUserWalletUseCase: GetUserWalletUseCase,
    singleAccountStatusSupplier: SingleAccountStatusSupplier,
    private val messageSender: UiMessageSender,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val archiveCryptoPortfolioUseCase: ArchiveCryptoPortfolioUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<AccountDetailsComponent.Params>()

    private val isMultiCurrency: Boolean = getUserWalletUseCase(params.account.accountId.userWalletId)
        .getOrNull()
        ?.isMultiCurrency == true

    private val accountId = params.account.accountId

    val jointSupportedNetworksNavigation: SlotNavigation<Unit> = SlotNavigation()

    val uiState: StateFlow<AccountDetailsUM>
        field = MutableStateFlow(
            when (val account = params.account) {
                is Account.Personal -> buildPersonalUI(account)
                is Account.Joint -> buildJointUI(account, status = null)
            },
        )

    init {
        analyticsEventHandler.send(AccountSettingsAnalyticEvents.AccountSettingsScreenOpened())

        when (params.account) {
            is Account.Personal -> singleAccountSupplier.filterPersonalAccount(accountId)
                .map { account -> buildPersonalUI(account) }
            is Account.Joint -> singleAccountStatusSupplier(SingleAccountStatusProducer.Params(accountId))
                .filterIsInstance<AccountStatus.Joint>()
                .map { status -> buildJointUI(status.account, status) }
        }
            .onEach { newState -> uiState.update { newState } }
            .launchIn(modelScope)
    }

    private fun onEditAccountClick(account: Account.Personal) {
        analyticsEventHandler.send(AccountSettingsAnalyticEvents.ButtonEdit())
        router.push(AppRoute.EditAccount(account))
    }

    private fun onManageTokensClick(account: Account.CryptoPortfolio) {
        val route = AppRoute.ManageTokens(
            source = AppRoute.ManageTokens.Source.ACCOUNT,
            accountId = account.accountId,
        )
        analyticsEventHandler.send(
            AccountSettingsAnalyticEvents.ButtonManageTokens(account.derivationIndex?.value),
        )
        router.push(route)
    }

    private fun onArchiveAccountClick(account: Account.CryptoPortfolio) {
        val accountDerivation = account.derivationIndex?.value
        val event = AccountSettingsAnalyticEvents.ButtonArchiveAccount(accountDerivation)
        analyticsEventHandler.send(event)
        confirmArchiveDialog(account)
    }

    private fun confirmArchiveDialog(account: Account.CryptoPortfolio) {
        val secondAction = EventMessageAction(
            title = resourceReference(R.string.common_cancel),
            onClick = {
                val accountDerivation = account.derivationIndex?.value
                val event = AccountSettingsAnalyticEvents.ButtonCancelAccountArchivation(accountDerivation)
                analyticsEventHandler.send(event)
            },
        )
        val firstAction = EventMessageAction(
            title = resourceReference(R.string.account_details_archive_action),
            isWarning = true,
            onClick = { archiveCryptoPortfolio(account) },
        )
        val descriptionRes = if (account is Account.Joint) {
            R.string.account_details_archive_joint_description
        } else {
            R.string.account_details_archive_description
        }
        messageSender.send(
            DialogMessage(
                title = resourceReference(R.string.account_details_archive),
                message = resourceReference(descriptionRes),
                firstActionBuilder = { firstAction },
                secondActionBuilder = { secondAction },
            ),
        )
    }

    private fun archiveCryptoPortfolio(account: Account.CryptoPortfolio) = modelScope.launch {
        val accountDerivation = account.derivationIndex?.value
        val event = AccountSettingsAnalyticEvents.ButtonArchiveAccountConfirmation(accountDerivation)
        analyticsEventHandler.send(event)
        uiState.update { it.toggleProgress(true) }
        archiveCryptoPortfolioUseCase(accountId)
            .onLeft { error ->
                failedArchiveDialog(error, account)
                uiState.update { it.toggleProgress(false) }
            }
            .onRight {
                analyticsEventHandler.send(AccountSettingsAnalyticEvents.AccountArchived())
                val message = resourceReference(R.string.account_archive_success_message)
                messageSender.send(ToastMessage(message = message))
                router.pop()
            }
    }

    private fun failedArchiveDialog(error: ArchiveCryptoPortfolioUseCase.Error, account: Account.CryptoPortfolio) {
        val event = AccountSettingsAnalyticEvents.AccountError(
            source = AccountSettingsAnalyticEvents.Source.ARCHIVE,
            error = error.tag,
            accountDerivation = account.derivationIndex?.value,
        )
        analyticsEventHandler.send(event)
        val titleRes: Int
        val messageRes: Int
        when (error) {
            is ArchiveCryptoPortfolioUseCase.Error.CriticalTechError.AccountListRequirementsNotMet,
            is ArchiveCryptoPortfolioUseCase.Error.CriticalTechError.AccountNotFound,
            is ArchiveCryptoPortfolioUseCase.Error.CriticalTechError.AccountsNotCreated,
            is ArchiveCryptoPortfolioUseCase.Error.DataOperationFailed,
            -> {
                titleRes = R.string.common_something_went_wrong
                messageRes = R.string.account_generic_error_dialog_message
            }
            is ArchiveCryptoPortfolioUseCase.Error.ActiveReferralStatus -> {
                titleRes = R.string.account_could_not_archive_referral_program_title
                messageRes = R.string.account_could_not_archive_referral_program_message
            }
        }

        val dialogMessage = DialogMessage(
            title = resourceReference(titleRes),
            message = resourceReference(messageRes),
        )
        messageSender.send(dialogMessage)
    }

    private fun buildPersonalUI(account: Account.Personal): AccountDetailsUM {
        val archiveMode = when (account.isMainAccount) {
            true -> ArchiveMode.None
            false -> ArchiveMode.Available(
                onArchiveAccountClick = { onArchiveAccountClick(account) },
                isLoading = false,
            )
        }
        return AccountDetailsUM(
            title = resourceReference(R.string.account_details_title),
            accountName = account.accountName.toUM().value,
            accountIcon = CryptoPortfolioIconConverter.convert(account.icon),
            onCloseClick = { router.pop() },
            onAccountEditClick = { onEditAccountClick(account) },
            onManageTokensClick = { onManageTokensClick(account) },
            archiveMode = archiveMode,
            isManageTokensAvailable = isMultiCurrency,
        )
    }

    private fun buildJointUI(account: Account.Joint, status: AccountStatus.Joint?): AccountDetailsUM {
        val isActive = status?.value is JointAccountStatusValue.Active
        return AccountDetailsUM(
            title = resourceReference(R.string.common_joint_account),
            accountName = account.accountName.toUM().value,
            accountIcon = CryptoPortfolioIconConverter.convert(account.icon),
            onCloseClick = { router.pop() },
            onAccountEditClick = null,
            onManageTokensClick = { onManageTokensClick(account) },
            archiveMode = ArchiveMode.Available(
                onArchiveAccountClick = { onArchiveAccountClick(account) },
                isLoading = false,
            ),
            isManageTokensAvailable = isActive && isMultiCurrency,
            isPendingActivation = !isActive,
            members = status?.value?.toMembersRowUM(onClick = { onMembersClick() }),
            networksInfo = if (isActive) {
                AccountDetailsUM.NetworksInfoUM(
                    text = resourceReference(R.string.account_details_joint_networks_info),
                    linkText = resourceReference(R.string.common_more_info),
                    onLinkClick = { jointSupportedNetworksNavigation.activate(Unit) },
                )
            } else {
                null
            },
        )
    }

    private fun JointAccountStatusValue.toMembersRowUM(onClick: () -> Unit): AccountDetailsUM.MembersRowUM? {
        val membersCount = when (this) {
            is JointAccountStatusValue.Pending -> resourceReference(
                id = R.string.account_details_joint_members_progress,
                formatArgs = wrappedList(joinedMembers, totalMembers),
            )
            is JointAccountStatusValue.Confirming -> pluralReference(
                id = R.plurals.account_details_joint_members_count,
                count = totalMembers,
                formatArgs = wrappedList(totalMembers),
            )
            is JointAccountStatusValue.Active -> pluralReference(
                id = R.plurals.account_details_joint_members_count,
                count = totalMembers,
                formatArgs = wrappedList(totalMembers),
            )
            JointAccountStatusValue.Loading,
            is JointAccountStatusValue.Error,
            -> return null
        }
        return AccountDetailsUM.MembersRowUM(
            title = resourceReference(R.string.joint_account_members_title),
            membersCount = membersCount,
            onClick = onClick,
        )
    }

    private fun onMembersClick() {
        router.push(
            AppRoute.JointAccountMembers(
                userWalletId = accountId.userWalletId,
                cryptoAccountId = accountId.value,
            ),
        )
    }

    private fun AccountDetailsUM.toggleProgress(isLoading: Boolean): AccountDetailsUM {
        val archiveMode = this.archiveMode as? ArchiveMode.Available ?: return this
        return this.copy(archiveMode = archiveMode.copy(isLoading = isLoading))
    }
}