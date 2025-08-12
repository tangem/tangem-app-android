package com.tangem.features.account.createedit

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.account.usecase.AddCryptoPortfolioUseCase
import com.tangem.domain.account.usecase.UpdateCryptoPortfolioUseCase
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.features.account.AccountCreateEditComponent
import com.tangem.features.account.createedit.entity.AccountCreateEditUM
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder.Companion.portfolioIcon
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder.Companion.updateButton
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder.Companion.updateColorSelect
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder.Companion.updateIconSelect
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder.Companion.updateName
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class AccountCreateEditModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val messageSender: UiMessageSender,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val updateCryptoPortfolioUseCase: UpdateCryptoPortfolioUseCase,
    private val addCryptoPortfolioUseCase: AddCryptoPortfolioUseCase,
) : Model() {

    private val params = paramsContainer.require<AccountCreateEditComponent.Params>()
    private val umBuilder = AccountCreateEditUMBuilder(params)

    val uiState: StateFlow<AccountCreateEditUM> get() = _uiState
    private val _uiState = MutableStateFlow(value = getInitialState())

    private fun unsaveChangeDialog() {
        val secondAction = EventMessageAction(
            title = resourceReference(R.string.account_unsaved_dialog_action_first),
            onClick = {},
        )
        val firstAction = EventMessageAction(
            title = resourceReference(R.string.account_unsaved_dialog_action_second),
            warning = true,
            onClick = { router.pop() },
        )
        messageSender.send(
            DialogMessage(
                title = resourceReference(R.string.account_unsaved_dialog_title),
                message = resourceReference(R.string.account_unsaved_dialog_message_create),
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
        val name = AccountName(state.account.name).getOrNull() ?: return
        val icon = state.account.portfolioIcon
        addCryptoPortfolioUseCase(
            userWalletId = params.userWalletId,
            accountName = name,
            icon = icon,
            derivationIndex = DerivationIndex.Main, // todo account
        )
    }

    private suspend fun editCryptoPortfolio(params: AccountCreateEditComponent.Params.Edit) {
        val state = uiState.value
        val name = AccountName(state.account.name).getOrNull() ?: return
        val icon = state.account.portfolioIcon
        val isNewName = name != params.account.name
        val isNewIcon = icon != params.account.portfolioIcon
        updateCryptoPortfolioUseCase(
            icon = if (isNewIcon) icon else null,
            accountName = if (isNewName) name else null,
            accountId = params.account.accountId,
        )
    }

    private fun onCloseClick() = unsaveChangeDialog()

    private fun onIconSelect(icon: CryptoPortfolioIcon.Icon) {
        _uiState.value = uiState.value
            .updateIconSelect(icon)
            .validateNewState()
    }

    private fun onColorSelect(color: CryptoPortfolioIcon.Color) {
        _uiState.value = uiState.value
            .updateColorSelect(color)
            .validateNewState()
    }

    private fun onNameChange(name: String) {
        _uiState.value = uiState.value
            .updateName(name)
            .validateNewState()
    }

    private fun AccountCreateEditUM.validateNewState(): AccountCreateEditUM {
        val isValidName = AccountName(this.account.name).isRight()
        val isAvailableForConfirm = when (params) {
            is AccountCreateEditComponent.Params.Create -> isValidName
            is AccountCreateEditComponent.Params.Edit -> {
                val isNewName = this.account.name != params.account.name.value
                val isNewIcon = this.account.portfolioIcon != params.account.portfolioIcon
                isValidName && (isNewName || isNewIcon)
            }
        }
        return this.updateButton(isButtonEnabled = isAvailableForConfirm)
    }

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
}