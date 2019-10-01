package com.tangem

abstract class Task(protected val delegate: CardManagerDelegate? = null, private val reader: CardReader) {

    abstract fun run(cardEnvironment: CardEnvironment,
                     callback: (result: TaskResult, cardEnvironment: CardEnvironment) -> Unit)

    protected fun executeCommand(command: Command, cardEnvironment: CardEnvironment,
                                 callback: (result: TaskResult) -> Unit) {

        reader.sendApdu(command.serialize(cardEnvironment).serialize()) {
            val parsedApdu = it.deserialize()
            if (parsedApdu == null) callback.invoke(TaskResult.Error(CardError()))
            else if (!parsedApdu.statusCompleted()) {
                callback.invoke(TaskResult.Error(CardError(parsedApdu.status.code)))
            } else {
                val response = command.deserialize(parsedApdu)
                callback.invoke(response)
            }
        }
    }
}


class BasicTask(
        private val command: Command,
        delegate: CardManagerDelegate? = null,
        reader: CardReader) : Task(delegate, reader) {

    override fun run(cardEnvironment: CardEnvironment,
                     callback: (result: TaskResult, cardEnvironment: CardEnvironment) -> Unit) {
        executeCommand(command, cardEnvironment) {
            callback.invoke(it, updateEnvironment())
        }
    }

    private fun updateEnvironment() : CardEnvironment{
// [REDACTED_TODO_COMMENT]
        return CardEnvironment()
    }
}


class SignTask(private val signCommandData: SignCommandData,
               delegate: CardManagerDelegate? = null,
               reader: CardReader) : Task(delegate, reader) {

    private lateinit var callback: (result: TaskResult, cardEnvironment: CardEnvironment) -> Unit

    override fun run(cardEnvironment: CardEnvironment,
                     callback: (result: TaskResult, cardEnvironment: CardEnvironment) -> Unit) {

        this.callback = callback

        delegate?.openNfcPopup()
        val signCommand = SignCommand(signCommandData)

        executeCommand(signCommand, cardEnvironment) {
            delegate?.closeNfcPopup()
            run { callback(it, updateEnvironment()) }
        }
    }

    private fun updateEnvironment() : CardEnvironment{
// [REDACTED_TODO_COMMENT]
        return CardEnvironment()
    }
}


class ScanTask(delegate: CardManagerDelegate? = null, reader: CardReader) : Task(delegate, reader) {

    private lateinit var card: Card

    override fun run(cardEnvironment: CardEnvironment,
                     callback: (result: TaskResult, cardEnvironment: CardEnvironment) -> Unit) {

        delegate?.openNfcPopup()
        val readCommand = ReadCardCommand(cardEnvironment.pin1)
        executeCommand(readCommand, cardEnvironment) {

            if (it is TaskResult.Success) {
                card = it.resultData as Card
                val checkWalletCommand = prepareCheckWalletCommand(cardEnvironment)

                executeCommand(checkWalletCommand, cardEnvironment) {
                    delegate?.closeNfcPopup()
// [REDACTED_TODO_COMMENT]
                    run { callback(it, updateEnvironment()) }
                }
            } else {
// [REDACTED_TODO_COMMENT]
            }
        }
    }

    private fun updateEnvironment() : CardEnvironment{
// [REDACTED_TODO_COMMENT]
        return CardEnvironment()
    }

    private fun prepareCheckWalletCommand(cardEnvironment: CardEnvironment): CheckWalletCommand {
        val challenge = generateChallenge()
        return CheckWalletCommand(
                cardEnvironment.pin1,
                card.cid,
                challenge,
                byteArrayOf())
    }

    private fun generateChallenge(): ByteArray {
        return byteArrayOf()
    }
}