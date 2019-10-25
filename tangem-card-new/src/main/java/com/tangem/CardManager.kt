package com.tangem

import com.tangem.commands.CommandResponse
import com.tangem.commands.CommandSerializer
import com.tangem.commands.SignCommand
import com.tangem.commands.SignResponse
import com.tangem.crypto.initCrypto
import com.tangem.tasks.*

class CardManager(
        private val reader: CardReader,
        private val cardManagerDelegate: CardManagerDelegate? = null,
        dataStorage: DataStorage? = null) {

    var isBusy = false
        private set

    private val cardEnvironmentRepository = mutableMapOf<String, CardEnvironment>()

    init {
        initCrypto()
    }

    fun scanCard(callback: (result: TaskEvent<ScanEvent>) -> Unit) {
        val task = ScanTask(cardManagerDelegate, reader)
        runTask(task, callback = callback)
    }

    fun sign(hashes: Array<ByteArray>, cardId: String,
             callback: (result: TaskEvent<SignResponse>) -> Unit) {
        var signCommand: SignCommand
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
        val task = SingleCommandTask(signCommand, cardManagerDelegate, reader)
        runTask(task, cardId, callback)
    }

    fun <T> runTask(task: Task<T>, cardId: String? = null,
                    callback: (result: TaskEvent<T>) -> Unit) {
//        AppExecutors.diskIO().execute {
        val environment = fetchCardEnvironment(cardId)
        isBusy = true
        task.run(environment, callback)
//        }
    }

    private fun fetchCardEnvironment(cardId: String?): CardEnvironment {
        return cardEnvironmentRepository[cardId] ?: CardEnvironment()
    }

    fun <T : CommandResponse> runCommand(commandSerializer: CommandSerializer<T>,
                                         cardId: String? = null,
                                         callback: (result: TaskEvent<T>) -> Unit) {
        val task = SingleCommandTask(commandSerializer, cardManagerDelegate, reader)
        runTask(task, cardId, callback)
    }
}