package com.tangem.features.hotwallet.forgetwallet

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.features.hotwallet.ForgetWalletComponent
import com.tangem.features.hotwallet.forgetwallet.entity.ForgetWalletUM
import com.tangem.features.hotwallet.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@ModelScoped
internal class ForgetWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<ForgetWalletComponent.Params>()

    internal val uiState: StateFlow<ForgetWalletUM>
        field = MutableStateFlow(
            ForgetWalletUM(
                onBackClick = { router.pop() },
                isCheckboxChecked = false,
                onCheckboxClick = ::onCheckboxClick,
                onForgetWalletClick = ::onForgetWalletClick,
                isForgetButtonEnabled = false,
            ),
        )

    private fun onCheckboxClick() {
        uiState.update { currentState ->
            val newValue = !currentState.isCheckboxChecked
            currentState.copy(
                isCheckboxChecked = newValue,
                isForgetButtonEnabled = newValue,
            )
        }
    }

    private fun onForgetWalletClick() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.common_attention),
                message = resourceReference(R.string.hw_remove_wallet_confirmation_title),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_forget),
                        isWarning = true,
                        onClick = ::forgetWallet,
                    )
                },
                secondActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_cancel),
                        onClick = {},
                    )
                },
            ),
        )
    }

    private fun forgetWallet() {
        modelScope.launch {
            val hasUserWallets = deleteWalletUseCase(params.userWalletId)
                .getOrElse {
                    Timber.e("Unable to delete wallet: $it")

                    uiMessageSender.send(
                        message = SnackbarMessage(resourceReference(R.string.common_unknown_error)),
                    )

                    return@launch
                }

            if (hasUserWallets) {
                router.popTo(AppRoute.Details::class)
            } else {
                router.replaceAll(AppRoute.Home())
            }
        }
    }
}