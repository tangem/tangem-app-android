package com.tangem.tasks

import com.tangem.CardEnvironment
import com.tangem.CardManagerDelegate
import com.tangem.CardReader
import com.tangem.commands.SignCommand
import com.tangem.commands.SignCommandData

class SignTask(private val signCommandData: SignCommandData,
               delegate: CardManagerDelegate? = null,
               reader: CardReader) : Task(delegate, reader) {

    private lateinit var callback: (result: TaskResult, cardEnvironment: CardEnvironment) -> Unit

    override fun onRun(cardEnvironment: CardEnvironment,
                       callback: (result: TaskResult, cardEnvironment: CardEnvironment) -> Unit) {

        this.callback = callback

        val signCommand = SignCommand(signCommandData)

        sendCommand(signCommand, cardEnvironment) {
            delegate?.closeNfcPopup()
            run { callback(it, updateEnvironment()) }
        }
    }

    private fun updateEnvironment(): CardEnvironment {
        //TODO: set logic of changing CardEnvironment when needed
        return CardEnvironment()
    }
}