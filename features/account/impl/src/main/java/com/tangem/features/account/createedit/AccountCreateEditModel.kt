package com.tangem.features.account.createedit

import com.tangem.common.ui.account.AccountNameUM
import com.tangem.common.ui.account.toDomain
import androidx.annotation.StringRes
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
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.ToastMessage
import com.tangem.core.ui.utils.showErrorDialog
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
            .onLeft { showMessage(it.toString()) }
            .onRight {
                showMessage(R.string.account_create_success_message)
                router.pop()
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
            .onLeft { showMessage(it.toString()) }
            .onRight {
                showMessage(R.string.account_edit_success_message)
                router.pop()
            }
    }

    private fun showMessage(@StringRes id: Int) {
        val message = resourceReference(id)
        messageSender.send(ToastMessage(message = message))
    }

    private fun showMessage(text: String) {
        messageSender.send(ToastMessage(message = stringReference(text)))
    }

    private fun onCloseClick() = unsaveChangeDialog()

    private fun onIconSelect(icon: CryptoPortfolioIcon.Icon) {
        uiState.value = uiState.value
            .updateIconSelect(icon)
            .validateNewState()
    }

    private fun onColorSelect(color: CryptoPortfolioIcon.Color) {
        uiState.value = uiState.value
            .updateColorSelect(color)
            .validateNewState()
    }

    private fun onNameChange(name: AccountNameUM) {
        uiState.value = uiState.value
            .updateName(name)
            .validateNewState()
    }

    private fun AccountCreateEditUM.validateNewState(): AccountCreateEditUM {
        val isValidName = this.account.name.toDomain().isRight()
        val isAvailableForConfirm = when (params) {
            is AccountCreateEditComponent.Params.Create -> isValidName
            is AccountCreateEditComponent.Params.Edit -> {
                val oldName = params.account.accountName.toUM()

                val isNewName = this.account.name != oldName
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

    private fun updateDerivationInfo(userWalletId: UserWalletId) {
        modelScope.launch(dispatchers.default) {
            getUnoccupiedAccountIndexUseCase(userWalletId = userWalletId)
                .onRight { derivationIndex ->
                    uiState.update {
                        it.updateDerivationIndex(derivationIndex = derivationIndex.value)
                    }
                }
                .onLeft { cause ->
                    handleError(
                        error = AccountFeatureError.CreateAccount.UnableToGetDerivationIndex,
                        message = cause.toString(),
                        params = mapOf(
                            "userWalletId" to userWalletId.stringValue,
                            "cause" to cause.toString(),
                        ),
                    )

                    return@launch
                }
        }
    }

    private fun handleError(
        error: AccountFeatureError,
        message: String? = null,
        params: Map<String, String> = mapOf(),
    ) {
        val exception = IllegalStateException("$error. Cause: $message")

        Timber.e(exception)

        analyticsExceptionHandler.sendException(
            event = ExceptionAnalyticsEvent(exception = exception, params = params),
        )

        messageSender.showErrorDialog(universalError = error, onDismiss = router::pop)
    }
}