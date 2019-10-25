package com.tangem.tasks

import com.tangem.CardEnvironment
import com.tangem.CardManagerDelegate
import com.tangem.CardReader
import com.tangem.commands.CommandResponse
import com.tangem.commands.CommandSerializer

class SingleCommandTask<Event : CommandResponse>(
        private val commandSerializer: CommandSerializer<Event>,
        delegate: CardManagerDelegate? = null,
        reader: CardReader) : Task<Event>(delegate, reader) {

    override fun onRun(cardEnvironment: CardEnvironment,
                       callback: (result: TaskEvent<Event>) -> Unit) {
        sendCommand(commandSerializer, cardEnvironment) {
            delegate?.closeNfcPopup()
            callback(it)
        }
    }
}

