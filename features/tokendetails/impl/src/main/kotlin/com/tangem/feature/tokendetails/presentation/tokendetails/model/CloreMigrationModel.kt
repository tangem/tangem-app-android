package com.tangem.feature.tokendetails.presentation.tokendetails.model

import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.SignCloreMessageError
import com.tangem.domain.transaction.usecase.SignCloreMessageUseCase
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsStateFactory
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// TODO: Remove after Clore migration ends ([REDACTED_TASK_KEY])
@Suppress("LongParameterList")
internal class CloreMigrationModel(
    private val stateFactory: TokenDetailsStateFactory,
    private val signCloreMessageUseCase: SignCloreMessageUseCase,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
    private val router: InnerTokenDetailsRouter,
    private val userWallet: UserWallet,
    private val cryptoCurrency: CryptoCurrency,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
    private val onStateUpdate: (TokenDetailsState) -> Unit,
) {

    private val state = MutableStateFlow(CloreMigrationState())

    fun onCloreMigrationClick() {
        state.value = CloreMigrationState()
        updateBottomSheet()
    }

    fun onCloreSignMessage(message: String) {
        if (message.isBlank()) return

        state.value = state.value.copy(message = message)
        updateBottomSheet(isSigningInProgress = true)

        coroutineScope.launch(dispatchers.main) {
            signCloreMessageUseCase(
                userWallet = userWallet,
                currency = cryptoCurrency,
                message = message,
            ).fold(
                ifLeft = { error ->
                    val errorMessage = when (error) {
                        is SignCloreMessageError.SigningFailed -> stringReference(error.message)
                        SignCloreMessageError.MessageSigningNotSupported ->
                            resourceReference(R.string.warning_clore_migration_error_signing_not_supported)
                        SignCloreMessageError.WalletManagerNotFound ->
                            resourceReference(R.string.warning_clore_migration_error_wallet_manager_not_found)
                    }
                    state.value = state.value.copy(signature = "")
                    updateBottomSheet()
                    uiMessageSender.send(SnackbarMessage(errorMessage))
                },
                ifRight = { signature ->
                    state.value = state.value.copy(signature = signature)
                    onStateUpdate(stateFactory.getStateWithUpdatedCloreMigrationSignature(signature))
                },
            )
        }
    }

    fun onOpenCloreClaimPortal() {
        router.openUrl(CLAIM_PORTAL_URL)
    }

    private fun updateBottomSheet(isSigningInProgress: Boolean = false) {
        val currentState = state.value
        onStateUpdate(
            stateFactory.getStateWithCloreMigrationBottomSheet(
                message = currentState.message,
                signature = currentState.signature,
                isSigningInProgress = isSigningInProgress,
                onMessageChange = { newMessage ->
                    state.value = state.value.copy(message = newMessage)
                    updateBottomSheet()
                },
                onSignClick = { onCloreSignMessage(state.value.message) },
                onCopyClick = {
                    val signature = state.value.signature
                    if (signature.isNotBlank()) {
                        clipboardManager.setText(text = signature, isSensitive = false)
                        uiMessageSender.send(
                            SnackbarMessage(resourceReference(R.string.wallet_notification_address_copied)),
                        )
                    }
                },
                onOpenPortalClick = { onOpenCloreClaimPortal() },
            ),
        )
    }

    private companion object {
        const val CLAIM_PORTAL_URL = "https://claim-portal.clore.ai/"
    }
}

private data class CloreMigrationState(
    val message: String = "",
    val signature: String = "",
)