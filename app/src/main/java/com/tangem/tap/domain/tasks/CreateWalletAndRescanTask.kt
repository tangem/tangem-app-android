package com.tangem.tap.domain.tasks

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.PreflightReadTask
import com.tangem.operations.wallet.CreateWalletCommand

class CreateWalletAndRescanTask : CardSessionRunnable<Card> {

    override fun run(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }
        val firmwareVersion = card.firmwareVersion

        val task = if (firmwareVersion < FirmwareVersion.MultiWalletAvailable) {
            CreateWalletCommand(card.supportedCurves.first())
        } else {
            CreateWalletsTask()
        }

        task.run(session) { result ->
            when (result) {
                is CompletionResult.Success ->
                    PreflightReadTask(PreflightReadMode.FullCardRead).run(session, callback)
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}