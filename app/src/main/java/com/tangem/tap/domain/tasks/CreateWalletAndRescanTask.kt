package com.tangem.tap.domain.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.FirmwareConstraints
import com.tangem.TangemSdkError
import com.tangem.commands.common.card.Card
import com.tangem.common.CompletionResult
import com.tangem.tasks.CreateWalletTask
import com.tangem.tasks.PreflightReadCapable
import com.tangem.tasks.PreflightReadSettings
import com.tangem.tasks.PreflightReadTask

class CreateWalletAndRescanTask : CardSessionRunnable<Card>, PreflightReadCapable {
    override val requiresPin2 = false
    override fun preflightReadSettings() = PreflightReadSettings.FullCardRead

    override fun run(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {
        val firmwareVerion = session.environment.card?.firmwareVersion
        if (firmwareVerion == null) {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }
        val task = if (firmwareVerion < FirmwareConstraints.AvailabilityVersions.walletData) {
            CreateWalletTask()
        } else {
            CreateWalletsTask()
        }

        task.run(session) { result ->
            when (result) {
                is CompletionResult.Success ->
                    PreflightReadTask(PreflightReadSettings.FullCardRead).run(session, callback)
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}