package com.tangem.feature.walletsettings.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.wallets.models.UpdateWalletError
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.RenameWalletUseCase
import com.tangem.feature.walletsettings.component.RenameWalletComponent
import com.tangem.feature.walletsettings.entity.RenameWalletUM
import com.tangem.feature.walletsettings.impl.R
import com.tangem.feature.walletsettings.ui.RenameWalletDialog
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

internal class DefaultRenameWalletComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: RenameWalletComponent.Params,
    private val renameWalletUseCase: RenameWalletUseCase,
) : RenameWalletComponent, AppComponentContext by context {

    private val currentWalletName = params.currentName

    private val stateFlow: MutableStateFlow<RenameWalletUM> = MutableStateFlow(
        value = RenameWalletUM(
            walletNameValue = TextFieldValue(text = params.currentName),
            isNameCorrect = false,
            updateValue = ::updateValue,
            onConfirm = { renameWallet(params.userWalletId) },
        ),
    )

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun Dialog() {
        val model by stateFlow.collectAsStateWithLifecycle()

        RenameWalletDialog(
            model = model,
            onDismiss = ::dismiss,
        )
    }

    private fun updateValue(value: TextFieldValue) {
        stateFlow.update {
            it.copy(
                walletNameValue = value,
                isNameCorrect = value.text.isNotBlank() && value.text != currentWalletName,
            )
        }
    }

    private fun renameWallet(userWalletId: UserWalletId) = componentScope.launch {
        val newName = stateFlow.value.walletNameValue
        val maybeError = renameWalletUseCase(userWalletId, newName.text).leftOrNull()

        if (maybeError != null) {
            Timber.e("Unable to rename wallet: $maybeError")

            val message = when (maybeError) {
                is UpdateWalletError.DataError -> resourceReference(
                    id = R.string.common_unknown_error,
                )
                is UpdateWalletError.NameAlreadyExists -> resourceReference(
                    id = R.string.user_wallet_list_rename_popup_error_already_exists,
                    formatArgs = wrappedList(newName),
                )
            }

            messageSender.send(message = SnackbarMessage(message))
        }

        dismiss()
    }

    @AssistedFactory
    interface Factory : RenameWalletComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: RenameWalletComponent.Params,
        ): DefaultRenameWalletComponent
    }
}
