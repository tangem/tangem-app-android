package com.tangem.tasks

import com.tangem.CardEnvironment
import com.tangem.commands.CommandResponse
import com.tangem.commands.CommandSerializer
import com.tangem.common.CompletionResult

class SingleCommandTask<Event : CommandResponse>(
        private val commandSerializer: CommandSerializer<Event>) : Task<Event>() {

    override fun onRun(cardEnvironment: CardEnvironment,
                       callback: (result: TaskEvent<Event>) -> Unit) {
        sendCommand(commandSerializer, cardEnvironment) { completionResult ->
            when (completionResult) {
                is CompletionResult.Success -> {
                    onTaskCompleted()
                    callback(TaskEvent.Event(completionResult.data))
                    callback(TaskEvent.Completion())
                }
                is CompletionResult.Failure -> {
                    if (completionResult.error !is TaskError.UserCancelledError) {
                        onTaskCompleted(true, completionResult.error)
                    }
                    callback(TaskEvent.Completion(completionResult.error))
                }
            }
        }
    }
}
