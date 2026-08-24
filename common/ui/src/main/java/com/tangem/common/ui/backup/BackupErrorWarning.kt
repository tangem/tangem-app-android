package com.tangem.common.ui.backup

import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.domain.account.status.usecase.GetBackupProblematicWalletForAddressUseCase
import com.tangem.domain.card.IsWalletBackupProblematicUseCase
import com.tangem.domain.feedback.SendBackupProblemEmailUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Warns that funds are about to reach a wallet with an incomplete backup — such a wallet should be reset
 * rather than topped up. Adding funds is not blocked: `onProceed` runs either right away or from the
 * alert's "Continue" action, while "Contact support" abandons the action for the mail flow.
 *
 * The message sender is assisted because components take it from their `AppComponentContext` rather
 * than from the DI graph.
 */
class BackupErrorWarning @AssistedInject constructor(
    @Assisted private val messageSender: UiMessageSender,
    private val isWalletBackupProblematicUseCase: IsWalletBackupProblematicUseCase,
    private val getBackupProblematicWalletForAddressUseCase: GetBackupProblematicWalletForAddressUseCase,
    private val sendBackupProblemEmailUseCase: SendBackupProblemEmailUseCase,
) {

    private val addressCheckJobHolder = JobHolder()

    /**
     * Checks the wallet being topped up.
     *
     * @param onWarningShown extra action to run when the warning is about to be shown
     * @param onSupportClick extra action to run when "Contact support" is clicked
     */
    fun forWallet(
        scope: CoroutineScope,
        userWallet: UserWallet,
        onWarningShown: () -> Unit = {},
        onSupportClick: () -> Unit = {},
        onProceed: () -> Unit,
    ) {
        if (!isWalletBackupProblematicUseCase(userWallet)) {
            onProceed()
            return
        }

        onWarningShown()
        showWarning(
            scope = scope,
            userWalletId = userWallet.walletId,
            onSupportClick = onSupportClick,
            onProceed = onProceed,
        )
    }

    /**
     * Checks the wallet the entered address belongs to, if it is one of the user's own wallets.
     *
     * [address] is read again before proceeding: the recipient stays editable while the lookup and the
     * alert are on screen, so an address changed in between is re-checked instead of riding on the
     * verdict for the previous one.
     */
    fun forAddress(scope: CoroutineScope, address: () -> String?, onProceed: () -> Unit) {
        scope.launch {
            val checkedAddress = address()
            val problematicWalletId = checkedAddress
                ?.takeIf(String::isNotEmpty)
                ?.let { getBackupProblematicWalletForAddressUseCase(it) }

            if (problematicWalletId == null) {
                proceedIfUnchanged(
                    scope = scope,
                    address = address,
                    checkedAddress = checkedAddress,
                    onProceed = onProceed,
                )
                return@launch
            }

            showWarning(
                scope = scope,
                userWalletId = problematicWalletId,
                onProceed = {
                    proceedIfUnchanged(
                        scope = scope,
                        address = address,
                        checkedAddress = checkedAddress,
                        onProceed = onProceed,
                    )
                },
            )
        }.saveIn(addressCheckJobHolder)
    }

    private fun proceedIfUnchanged(
        scope: CoroutineScope,
        address: () -> String?,
        checkedAddress: String?,
        onProceed: () -> Unit,
    ) {
        if (address() == checkedAddress) {
            onProceed()
        } else {
            forAddress(scope = scope, address = address, onProceed = onProceed)
        }
    }

    private fun showWarning(
        scope: CoroutineScope,
        userWalletId: UserWalletId,
        onSupportClick: () -> Unit = {},
        onProceed: () -> Unit,
    ) {
        messageSender.send(
            Dialogs.backupErrorAddFundsWarning(
                onContinue = onProceed,
                onContactSupport = {
                    onSupportClick()
                    scope.launch { sendBackupProblemEmailUseCase(userWalletId) }
                },
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(messageSender: UiMessageSender): BackupErrorWarning
    }
}