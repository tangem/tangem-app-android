package com.tangem.feature.walletsettings.component.impl.model

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.wallets.models.UpdateWalletError
import com.tangem.domain.wallets.usecase.GetWalletNamesUseCase
import com.tangem.domain.wallets.usecase.RenameWalletUseCase
import com.tangem.feature.walletsettings.component.RenameWalletComponent
import com.tangem.feature.walletsettings.entity.RenameWalletUM
import com.tangem.feature.walletsettings.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Rename wallet model
 *
 * @param paramsContainer        params container
 * @param getWalletNamesUseCase  use case for getting wallets names
 * @property dispatchers         dispatchers
 * @property renameWalletUseCase use case for renaming the wallet by id
 * @property uiMessageSender     UI message sender
 *
[REDACTED_AUTHOR]
 */
@ComponentScoped
internal class RenameWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    getWalletNamesUseCase: GetWalletNamesUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
    private val renameWalletUseCase: RenameWalletUseCase,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params: RenameWalletComponent.Params = paramsContainer.require()

    private val initialName = params.currentName
    private val walletNames by lazy(getWalletNamesUseCase::invoke)

    val state: StateFlow<RenameWalletUM> get() = _state

    private val _state = MutableStateFlow(
        value = RenameWalletUM(
            walletNameValue = TextFieldValue(text = params.currentName),
            onValueChange = ::onValueChange,
            isConfirmEnabled = false,
            onConfirmClick = ::onConfirmClick,
            error = null,
        ),
    )

    fun dismiss() {
        params.onDismiss()
    }

    private fun onValueChange(value: TextFieldValue) {
        _state.update {
            val newName = value.text

            it.copy(
                walletNameValue = value,
                isConfirmEnabled = getConfirmButtonAvailability(newName),
                error = getErrorOrNull(newName),
            )
        }
    }

    private fun getConfirmButtonAvailability(newName: String): Boolean {
        return newName.isNotBlank() && newName != initialName && !isNameAlreadyExists(newName)
    }

    private fun getErrorOrNull(newName: String): TextReference? {
        return if (isNameAlreadyExists(newName)) {
            resourceReference(
                id = R.string.user_wallet_list_rename_popup_error_already_exists,
                formatArgs = wrappedList(newName),
            )
        } else {
            null
        }
    }

    private fun isNameAlreadyExists(newName: String): Boolean {
        return initialName != newName && walletNames.contains(newName)
    }

    private fun onConfirmClick() {
        modelScope.launch {
            withContext(NonCancellable) {
                val newName = _state.value.walletNameValue

                renameWalletUseCase(userWalletId = params.userWalletId, name = newName.text)
                    .onLeft {
                        Timber.e("Unable to rename wallet: $it")
                        showRenameWalletError(error = it, updatedName = newName.text)
                    }
            }
        }

        dismiss()
    }

    private fun showRenameWalletError(error: UpdateWalletError, updatedName: String) {
        val message = when (error) {
            is UpdateWalletError.DataError -> resourceReference(id = R.string.common_unknown_error)
            is UpdateWalletError.NameAlreadyExists -> resourceReference(
                id = R.string.user_wallet_list_rename_popup_error_already_exists,
                formatArgs = wrappedList(updatedName),
            )
        }

        uiMessageSender.send(message = SnackbarMessage(message))
    }
}