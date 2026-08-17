package com.tangem.tap.domain.tasks.product

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.backup.ResetBackupCommand
import com.tangem.operations.masterSecret.PurgeMasterSecretCommand
import com.tangem.operations.read.ReadMasterSecretCommand
import com.tangem.operations.securechannel.manageAccessTokens.ResetAccessTokensTask
import com.tangem.operations.wallet.PurgeWalletCommand

class ResetToFactorySettingsTask(
    override val allowsRequestAccessCodeFromRepository: Boolean,
) : CardSessionRunnable<Boolean> {

    private var isResetCompleted = false

    /**
     * [deleteMasterSecret] decides whether to run [PurgeMasterSecretCommand] based on `card.masterSecret`,
     * which the default [PreflightReadMode.FullCardRead] no longer populates on COS v8+ cards. Requesting
     * [PreflightReadMode.FullCardRead.Option.ReadMasterSecret] here loads it during preflight when this task
     * is the top-level runnable; when the task runs inside another task's session (where this override is
     * ignored), [deleteMasterSecret] falls back to reading it explicitly.
     */
    override fun preflightReadMode(): PreflightReadMode =
        PreflightReadMode.FullCardRead(setOf(PreflightReadMode.FullCardRead.Option.ReadMasterSecret))

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
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        if (card.firmwareVersion < FirmwareVersion.v8) {
            resetBackup(session, callback)
            return
        }

        // When this task runs inside another task's session, the top-level preflight may not have loaded
        // the master secret (the default FullCardRead() doesn't) — read it here before deciding to purge
        if (card.masterSecret == null) {
            ReadMasterSecretCommand().run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        session.environment.card = session.environment.card?.copy(
                            masterSecret = result.data.masterSecret,
                        )
                        purgeMasterSecretIfNeeded(session, callback)
                    }
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
            return
        }

        purgeMasterSecretIfNeeded(session, callback)
    }

    private fun purgeMasterSecretIfNeeded(session: CardSession, callback: (result: CompletionResult<Boolean>) -> Unit) {
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