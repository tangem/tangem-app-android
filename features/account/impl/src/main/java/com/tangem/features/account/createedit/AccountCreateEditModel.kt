package com.tangem.features.account.createedit

import androidx.annotation.StringRes
import com.tangem.common.ui.account.AccountNameUM
import com.tangem.common.ui.account.toDomain
import com.tangem.common.ui.account.toUM
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
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
import com.tangem.core.ui.utils.showErrorDialog
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.usecase.AddCryptoPortfolioUseCase
import com.tangem.domain.account.usecase.GetUnoccupiedAccountIndexUseCase
import com.tangem.domain.account.usecase.UpdateCryptoPortfolioUseCase
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.account.AccountCreateEditComponent
import com.tangem.features.account.createedit.entity.AccountCreateEditUM
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder.Companion.portfolioIcon
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder.Companion.toggleProgress
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder.Companion.updateButton
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder.Companion.updateColorSelect
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder.Companion.updateDerivationIndex
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder.Companion.updateIconSelect
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder.Companion.updateName
import com.tangem.features.account.createedit.error.AccountFeatureError
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class AccountCreateEditModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val messageSender: UiMessageSender,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val updateCryptoPortfolioUseCase: UpdateCryptoPortfolioUseCase,
    private val addCryptoPortfolioUseCase: AddCryptoPortfolioUseCase,
    private val getUnoccupiedAccountIndexUseCase: GetUnoccupiedAccountIndexUseCase,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
) : Model() {

    private val params = paramsContainer.require<AccountCreateEditComponent.Params>()
    private val umBuilder = AccountCreateEditUMBuilder(params)

    val uiState: StateFlow<AccountCreateEditUM>
        field = MutableStateFlow(value = getInitialState())

    init {
        if (params is AccountCreateEditComponent.Params.Create) {
            updateDerivationInfo(userWalletId = params.userWalletId)
        }
    }

    private fun unsaveChangeDialog() {
        val secondAction = EventMessageAction(
            title = resourceReference(R.string.account_unsaved_dialog_action_first),
            onClick = {},
        )
        val firstAction = EventMessageAction(
            title = resourceReference(R.string.account_unsaved_dialog_action_second),
            isWarning = true,
            onClick = { router.pop() },
        )
        val messageRes = when (params) {
            is AccountCreateEditComponent.Params.Create -> R.string.account_unsaved_dialog_message_create
            is AccountCreateEditComponent.Params.Edit -> R.string.account_unsaved_dialog_message_edit
        }
        messageSender.send(
            DialogMessage(
                title = resourceReference(R.string.account_unsaved_dialog_title),
                message = resourceReference(messageRes),
                firstActionBuilder = { firstAction },
                secondActionBuilder = { secondAction },
            ),
        )
    }

    private fun onConfirmClick() = modelScope.launch {
        when (params) {
            is AccountCreateEditComponent.Params.Create -> createNewCryptoPortfolio(params)
            is AccountCreateEditComponent.Params.Edit -> editCryptoPortfolio(params)
        }
    }

    private suspend fun createNewCryptoPortfolio(params: AccountCreateEditComponent.Params.Create) {
        val state = uiState.value
        val name = state.account.name.toDomain().getOrNull() ?: return
        val icon = state.account.portfolioIcon.toDomain()
        val index = state.account.derivationInfo.index ?: return
        val derivationIndex = DerivationIndex(value = index).getOrNull() ?: return

        uiState.value = uiState.value.toggleProgress(showProgress = true)
        val result = addCryptoPortfolioUseCase(
            userWalletId = params.userWalletId,
            accountName = name,
            icon = icon,
            derivationIndex = derivationIndex,
        )
        uiState.value = uiState.value.toggleProgress(showProgress = false)

        result
            .onLeft(::handleAddAccountError)
            .onRight {
                showMessage(R.string.account_create_success_message)
                router.pop()
            }
    }

    private fun handleAddAccountError(error: AddCryptoPortfolioUseCase.Error) {
        val isDuplicateAccountNamesError = (error as? AddCryptoPortfolioUseCase.Error.AccountListRequirementsNotMet)
            ?.cause is AccountList.Error.DuplicateAccountNames
        when {
            isDuplicateAccountNamesError -> showAccountNameExist()
            else -> {
                showSomethingWrong()
                logError(error = AccountFeatureError.CreateAccount.FailedToCreateAccount(cause = error))
            }
        }
    }

    private suspend fun editCryptoPortfolio(params: AccountCreateEditComponent.Params.Edit) {
        val state = uiState.value
        val name = state.account.name.toDomain().getOrNull() ?: return
        val icon = state.account.portfolioIcon.toDomain()
        val isNewName = name != params.account.accountName
        val isNewIcon = icon != params.account.portfolioIcon

        uiState.value = uiState.value.toggleProgress(showProgress = true)
        val result = updateCryptoPortfolioUseCase(
            icon = if (isNewIcon) icon else null,
            accountName = if (isNewName) name else null,
            accountId = params.account.accountId,
        )
        uiState.value = uiState.value.toggleProgress(showProgress = false)

        result
            .onLeft(::handleEditAccountError)
            .onRight {
                showMessage(R.string.account_edit_success_message)
                router.pop()
            }
    }

    private fun handleEditAccountError(error: UpdateCryptoPortfolioUseCase.Error) {
        val isDuplicateAccountNamesError = (error as? UpdateCryptoPortfolioUseCase.Error.AccountListRequirementsNotMet)
            ?.cause is AccountList.Error.DuplicateAccountNames
        when {
            isDuplicateAccountNamesError -> showAccountNameExist()
            else -> {
                showSomethingWrong()
                logError(error = AccountFeatureError.EditAccount.FailedToEditAccount(cause = error))
            }
        }
    }

    private fun showMessage(@StringRes id: Int) {
        val message = resourceReference(id)
        messageSender.send(ToastMessage(message = message))
    }

    private fun onCloseClick() {
        val shouldShowConfirmDialog = uiState.value.buttonState.isButtonEnabled
        if (shouldShowConfirmDialog) unsaveChangeDialog() else router.pop()
    }

    private fun onIconSelect(icon: CryptoPortfolioIcon.Icon) {
        uiState.update { currentState ->
            currentState.updateIconSelect(icon)
                .validateNewState()
        }
    }

    private fun onColorSelect(color: CryptoPortfolioIcon.Color) {
        uiState.update { currentState ->
            currentState.updateColorSelect(color)
                .validateNewState()
        }
    }

    private fun onNameChange(name: AccountNameUM) {
        val isNotEmptyCustomName = (name as? AccountNameUM.Custom)?.raw?.isNotEmpty() == true
        if (!name.isValidName() && isNotEmptyCustomName) {
            Timber.d("Invalid account name: $name")
            return
        }

        uiState.update { currentState ->
            currentState.updateName(name)
                .validateNewState()
        }
    }

    private fun AccountCreateEditUM.validateNewState(): AccountCreateEditUM {
        val isValidName = this.account.name.isValidName()
        val isAvailableForConfirm = when (params) {
            is AccountCreateEditComponent.Params.Create -> isValidName
            is AccountCreateEditComponent.Params.Edit -> {
                val oldName = params.account.accountName.toUM()

                val isNewName = this.account.name.trim() != oldName
                val isNewIcon = this.account.portfolioIcon != params.account.portfolioIcon.toUM()
                isValidName && (isNewName || isNewIcon)
            }
        }
        return this.updateButton(isButtonEnabled = isAvailableForConfirm)
    }

    private fun AccountNameUM.isValidName(): Boolean = this.toDomain().isRight()

    private fun getInitialState(): AccountCreateEditUM {
        return AccountCreateEditUM(
            title = umBuilder.toolbarTitle,
            account = umBuilder.initAccountUM(::onNameChange),
            colorsState = umBuilder.initColorsUM(::onColorSelect),
            iconsState = umBuilder.initIconsUM(::onIconSelect),
            buttonState = umBuilder.initButtonUM(::onConfirmClick),
            onCloseClick = ::onCloseClick,
        )
    }

    private fun updateDerivationInfo(userWalletId: UserWalletId) {
        modelScope.launch(dispatchers.default) {
            getUnoccupiedAccountIndexUseCase(userWalletId = userWalletId)
                .onRight { derivationIndex ->
                    uiState.update {
                        it.updateDerivationIndex(derivationIndex = derivationIndex.value)
                    }
                }
                .onLeft { cause ->
                    val error = AccountFeatureError.CreateAccount.UnableToGetDerivationIndex(cause)

                    logError(
                        error = error,
                        params = mapOf(
                            "userWalletId" to userWalletId.stringValue,
                            "cause" to cause.toString(),
                        ),
                    )

                    messageSender.showErrorDialog(universalError = error, onDismiss = router::pop)

                    return@launch
                }
        }
    }

    private fun logError(error: AccountFeatureError, params: Map<String, String> = emptyMap()) {
        val exception = IllegalStateException(error.toString())

        Timber.e(exception)

        analyticsExceptionHandler.sendException(
            event = ExceptionAnalyticsEvent(exception = exception, params = params),
        )
    }

    private fun showSomethingWrong() {
        val dialogMessage = DialogMessage(
            title = resourceReference(R.string.common_something_went_wrong),
            message = resourceReference(R.string.account_could_not_create),
        )
        messageSender.send(dialogMessage)
    }

    private fun showAccountNameExist() {
        val dialogMessage = DialogMessage(
            title = resourceReference(R.string.common_something_went_wrong),
            message = resourceReference(R.string.account_form_name_already_exist_error_description),
        )
        messageSender.send(dialogMessage)
    }
}

private fun AccountNameUM.trim(): AccountNameUM = when (this) {
    is AccountNameUM.Custom -> this.toDomain().getOrNull()?.toUM() ?: this
    AccountNameUM.DefaultMain -> this
}