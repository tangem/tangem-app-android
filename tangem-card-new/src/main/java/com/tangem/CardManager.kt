package com.tangem

import com.tangem.commands.CommandResponse
import com.tangem.commands.CommandSerializer
import com.tangem.commands.SignCommand
import com.tangem.commands.SignResponse
import com.tangem.crypto.initCrypto
import com.tangem.tasks.*

class CardManager(
        private val reader: CardReader,
        private val cardManagerDelegate: CardManagerDelegate? = null) {

    private var isBusy = false
        private set

    private val cardEnvironmentRepository = mutableMapOf<String, CardEnvironment>()

    init {
        initCrypto()
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
//        AppExecutors.diskIO().execute {

        if (isBusy) {
            callback(TaskEvent.Completion(TaskError.Busy()))
            return
        }

        val environment = fetchCardEnvironment(cardId)
        isBusy = true

        task.reader = reader
        task.delegate = cardManagerDelegate

        task.run(environment) {
            when (it) {
                is TaskEvent.Event -> callback(it)
                is TaskEvent.Completion -> {
                    isBusy = false
                    callback(it)
                }
            }
        }
//        }
    }

    private fun fetchCardEnvironment(cardId: String?): CardEnvironment {
        return cardEnvironmentRepository[cardId] ?: CardEnvironment()
    }

    fun <T : CommandResponse> runCommand(commandSerializer: CommandSerializer<T>,
                                         cardId: String? = null,
                                         callback: (result: TaskEvent<T>) -> Unit) {
        val task = SingleCommandTask(commandSerializer)
        runTask(task, cardId, callback)
    }
}