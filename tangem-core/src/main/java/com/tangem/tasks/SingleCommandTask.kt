package com.tangem.tasks

import com.tangem.CardEnvironment
import com.tangem.commands.CommandResponse
import com.tangem.commands.CommandSerializer
import com.tangem.common.CompletionResult

/**
 * Allows to perform a single command.
 *
 * @property command A command that will be performed.
 */
class SingleCommandTask<Event : CommandResponse>(
        private val command: CommandSerializer<Event>
) : Task<Event>() {

    override fun onRun(cardEnvironment: CardEnvironment,
                       callback: (result: TaskEvent<Event>) -> Unit) {
        sendCommand(command, cardEnvironment) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    completeNfcSession()
                    callback(TaskEvent.Event(result.data))
                    callback(TaskEvent.Completion())
                }
                is CompletionResult.Failure -> {
                    if (result.error !is TaskError.UserCancelledError) {
                        completeNfcSession(true, result.error)
                    }
                    callback(TaskEvent.Completion(result.error))
                }
            }
        }
    }
}
