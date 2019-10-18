package com.tangem

import com.tangem.commands.CommandResponse
import com.tangem.commands.CommandSerializer
import com.tangem.commands.SignCommandData
import com.tangem.tasks.*

class CardManager(
        private val reader: CardReader,
        private val cardManagerDelegate: CardManagerDelegate? = null,
        dataStorage: DataStorage? = null) {

    private val cardEnvironmentRepository = CardEnvironmentRepository(dataStorage)

    fun scanCard(callback: (result: TaskResult) -> Unit) {
        val task = ScanTask(cardManagerDelegate, reader)
        runTask(task, cardEnvironmentRepository.cardEnvironment, callback)
    }

    fun sign(signCommandData: SignCommandData,
             callback: (result: TaskResult) -> Unit) {
        val task = SignTask(signCommandData, cardManagerDelegate, reader)
        runTask(task, cardEnvironmentRepository.cardEnvironment, callback)
    }

    fun runTask(task: Task, environment: CardEnvironment? = null,
                            callback: (result: TaskResult) -> Unit) {
        task.run(environment ?: cardEnvironmentRepository.cardEnvironment) {result, returnedEnvironment ->
            //TODO: environment is kept in Repository
            cardEnvironmentRepository.cardEnvironment = returnedEnvironment
            callback.invoke(result)
        }
    }

    fun runCommand(commandSerializer: CommandSerializer<CommandResponse>,
                   environment: CardEnvironment? = null,
                   callback: (result: TaskResult) -> Unit) {
        val task = BasicTask<CommandResponse>(commandSerializer, cardManagerDelegate, reader)
        runTask(task, environment, callback)
    }
}