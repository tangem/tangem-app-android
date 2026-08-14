package com.tangem.tap.domain.tasks.product

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.backup.ResetBackupCommand
import com.tangem.operations.masterSecret.PurgeMasterSecretCommand
import com.tangem.operations.securechannel.manageAccessTokens.ResetAccessTokensTask
import com.tangem.operations.wallet.PurgeWalletCommand

class ResetToFactorySettingsTask(
    override val allowsRequestAccessCodeFromRepository: Boolean,
) : CardSessionRunnable<Boolean> {

    private var isResetCompleted = false

    override fun run(session: CardSession, callback: (result: CompletionResult<Boolean>) -> Unit) {
        deleteWallets(session, callback)
    }

    private fun deleteWallets(session: CardSession, callback: (result: CompletionResult<Boolean>) -> Unit) {
        val wallet = session
            .environment
            .card
            ?.wallets
            ?.lastOrNull()
            .guard {
                deleteMasterSecret(session, callback)
                return
            }

        PurgeWalletCommand(wallet.index).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    isResetCompleted = true
                    deleteWallets(session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun deleteMasterSecret(session: CardSession, callback: (result: CompletionResult<Boolean>) -> Unit) {
        if (session.environment.card?.masterSecret == null) {
            resetBackup(session, callback)
            return
        }

        PurgeMasterSecretCommand().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    isResetCompleted = true
                    resetBackup(session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun resetBackup(session: CardSession, callback: (result: CompletionResult<Boolean>) -> Unit) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        if (card.backupStatus == null || card.backupStatus == Card.BackupStatus.NoBackup) {
            // card reset access tokens after reset backup, so we can skip this step if backup is not required
            resetAccessTokens(session, callback)
            return
        }

        ResetBackupCommand().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    callback(CompletionResult.Success(true))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun resetAccessTokens(session: CardSession, callback: (result: CompletionResult<Boolean>) -> Unit) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        if (card.firmwareVersion < FirmwareVersion.v8) {
            callback(CompletionResult.Success(isResetCompleted))
            return
        }

        // Nothing to reset if backup required and backup is not done, so we can skip this step
        if (card.settings.isBackupRequired) {
            callback(CompletionResult.Success(isResetCompleted))
            return
        }

        ResetAccessTokensTask().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    callback(CompletionResult.Success(true))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}