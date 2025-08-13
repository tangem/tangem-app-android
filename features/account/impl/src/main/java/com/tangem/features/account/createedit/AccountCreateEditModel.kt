package com.tangem.features.account.createedit

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
import com.tangem.core.ui.utils.showErrorDialog
import com.tangem.domain.account.usecase.AddCryptoPortfolioUseCase
import com.tangem.domain.account.usecase.GetUnoccupiedAccountIndexUseCase
import com.tangem.domain.account.usecase.UpdateCryptoPortfolioUseCase
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.account.AccountCreateEditComponent
import com.tangem.features.account.common.toDomain
import com.tangem.features.account.createedit.entity.AccountCreateEditUM
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder
import com.tangem.features.account.createedit.entity.AccountCreateEditUMBuilder.Companion.portfolioIcon
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
        val name = AccountName(value = state.account.name).getOrNull() ?: return
        val icon = state.account.portfolioIcon.toDomain()
        val index = state.account.derivationInfo.index ?: return
        val derivationIndex = DerivationIndex(value = index).getOrNull() ?: return

        addCryptoPortfolioUseCase(
            userWalletId = params.userWalletId,
            accountName = name,
            icon = icon,
            derivationIndex = derivationIndex,
        )
    }

    private suspend fun editCryptoPortfolio(params: AccountCreateEditComponent.Params.Edit) {
        val state = uiState.value
        val name = AccountName(state.account.name).getOrNull() ?: return
        val icon = state.account.portfolioIcon.toDomain()
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
        uiState.value = uiState.value
            .updateIconSelect(icon)
            .validateNewState()
    }

    private fun onColorSelect(color: CryptoPortfolioIcon.Color) {
        uiState.value = uiState.value
            .updateColorSelect(color)
            .validateNewState()
    }

    private fun onNameChange(name: String) {
        uiState.value = uiState.value
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

    private fun updateDerivationInfo(userWalletId: UserWalletId) {
        modelScope.launch(dispatchers.default) {
            getUnoccupiedAccountIndexUseCase(userWalletId = userWalletId)
                .onRight { derivationIndex ->
                    uiState.update {
                        it.updateDerivationIndex(derivationIndex = derivationIndex.value)
                    }
                }
                .onLeft {
                    handleError(
                        error = AccountFeatureError.CreateAccount.UnableToGetDerivationIndex,
                        params = mapOf("userWalletId" to userWalletId.stringValue),
                    )

                    return@launch
                }
        }
    }

    private fun handleError(error: AccountFeatureError, params: Map<String, String> = mapOf()) {
        val exception = IllegalStateException(error.toString())

        Timber.e(exception)

        analyticsExceptionHandler.sendException(
            event = ExceptionAnalyticsEvent(exception = exception, params = params),
        )

        messageSender.showErrorDialog(universalError = error, onDismiss = router::pop)
    }
}