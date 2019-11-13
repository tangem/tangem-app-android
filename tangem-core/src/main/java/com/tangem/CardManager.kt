package com.tangem

import com.tangem.commands.CommandResponse
import com.tangem.commands.CommandSerializer
import com.tangem.commands.SignCommand
import com.tangem.commands.SignResponse
import com.tangem.crypto.CryptoUtils
import com.tangem.tasks.*
import java.util.concurrent.Executors

class CardManager(
        private val reader: CardReader,
        private val cardManagerDelegate: CardManagerDelegate? = null) {

    private var isBusy = false
    private val cardEnvironmentRepository = mutableMapOf<String, CardEnvironment>()
    private val cardManagerExecutor = Executors.newSingleThreadExecutor()

    init {
        CryptoUtils.initCrypto()
    }

    fun scanCard(callback: (result: TaskEvent<ScanEvent>) -> Unit) {
        val task = ScanTask()
        runTask(task, callback = callback)
    }

    fun sign(hashes: Array<ByteArray>, cardId: String,
             callback: (result: TaskEvent<SignResponse>) -> Unit) {
        val signCommand: SignCommand
        try {
            signCommand = SignCommand(hashes, cardId)
        } catch (error: Exception) {
            if (error is TaskError) {
                callback(TaskEvent.Completion(error))
            } else {
                callback(TaskEvent.Completion(TaskError.GenericError(error.message)))
            }
            return
        }
        val task = SingleCommandTask(signCommand)
        runTask(task, cardId, callback)
    }

    fun <T> runTask(task: Task<T>, cardId: String? = null,
                    callback: (result: TaskEvent<T>) -> Unit) {
        if (isBusy) {
            callback(TaskEvent.Completion(TaskError.Busy()))
            return
        }

        val environment = fetchCardEnvironment(cardId)
        isBusy = true

        task.reader = reader
        task.delegate = cardManagerDelegate

        cardManagerExecutor.execute {
            task.run(environment) { taskEvent ->
                if (taskEvent is TaskEvent.Completion) isBusy = false
                callback(taskEvent)
            }
        }
    }

    fun <T : CommandResponse> runCommand(command: CommandSerializer<T>,
                                         cardId: String? = null,
                                         callback: (result: TaskEvent<T>) -> Unit) {
        val task = SingleCommandTask(command)
        runTask(task, cardId, callback)
    }

    private fun fetchCardEnvironment(cardId: String?): CardEnvironment {
        return cardEnvironmentRepository[cardId] ?: CardEnvironment()
    }
}