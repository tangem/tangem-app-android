package com.tangem.tasks

import com.tangem.CardEnvironment
import com.tangem.commands.CommandResponse
import com.tangem.commands.CommandSerializer
import com.tangem.common.CompletionResult

class SingleCommandTask<Event : CommandResponse>(
        private val commandSerializer: CommandSerializer<Event>) : Task<Event>() {

    override fun onRun(cardEnvironment: CardEnvironment,
                       callback: (result: TaskEvent<Event>) -> Unit) {
        sendCommand(commandSerializer, cardEnvironment) {
            when (it) {
                is CompletionResult.Success -> {
                    delegate?.onTaskCompleted()
                    callback(TaskEvent.Event(it.data))
                    callback(TaskEvent.Completion())
                }
                is CompletionResult.Failure -> {
                    if (it.error !is TaskError.UserCancelledError) delegate?.onTaskError()
                    callback(TaskEvent.Completion(it.error))
                }
            }
        }
    }
}
