package com.tangem

import com.tangem.commands.CommandResponse
import com.tangem.commands.CommandSerializer
import com.tangem.commands.SignCommandData
import com.tangem.crypto.initCrypto
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.ScanTask
import com.tangem.tasks.Task
import com.tangem.tasks.TaskResult

class CardManager(
        private val reader: CardReader,
        private val cardManagerDelegate: CardManagerDelegate? = null,
        dataStorage: DataStorage? = null) {

    private val cardEnvironmentRepository = CardEnvironmentRepository(dataStorage)

    init {
        initCrypto()
    }

    fun scanCard(callback: (result: ScanEvent, cardEnvironment: CardEnvironment) -> Unit) {
        val task = ScanTask(cardManagerDelegate, reader)
        runTask(task, cardEnvironmentRepository.cardEnvironment, callback)
    }

    fun sign(signCommandData: SignCommandData,
             callback: (result: TaskResult) -> Unit) {
//        val task = SignTask<SignEvent>(signCommandData, cardManagerDelegate, reader)
//        runTask(task, cardEnvironmentRepository.cardEnvironment, callback)
    }

    fun <TaskEvent>runTask(task: Task<TaskEvent>, environment: CardEnvironment? = null,
                            callback: (result: TaskEvent, cardEnvironment: CardEnvironment) -> Unit) {
        task.run(environment ?: cardEnvironmentRepository.cardEnvironment, callback)
    }

    fun runCommand(commandSerializer: CommandSerializer<CommandResponse>,
                   environment: CardEnvironment? = null,
                   callback: (result: TaskResult) -> Unit) {
//        val task = BasicTask<CommandResponse>(commandSerializer, cardManagerDelegate, reader)
//        runTask(task, environment, callback)
    }
}