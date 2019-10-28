package com.tangem.tasks

import com.tangem.CardEnvironment
import com.tangem.commands.CommandResponse
import com.tangem.commands.CommandSerializer

class SingleCommandTask<Event : CommandResponse>(
        private val commandSerializer: CommandSerializer<Event>) : Task<Event>() {

    override fun onRun(cardEnvironment: CardEnvironment,
                       callback: (result: TaskEvent<Event>) -> Unit) {
        sendCommand(commandSerializer, cardEnvironment) {
            delegate?.closeNfcPopup()
            callback(it)
        }
    }
}
