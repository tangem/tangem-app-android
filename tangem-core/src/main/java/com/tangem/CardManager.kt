package com.tangem

import com.tangem.commands.CommandResponse
import com.tangem.commands.CommandSerializer
import com.tangem.commands.SignCommand
import com.tangem.commands.SignResponse
import com.tangem.crypto.CryptoUtils
import com.tangem.tasks.*
import java.util.concurrent.Executors

/**
 * The main interface of Tangem SDK that allows your app to communicate with Tangem cards.
 *
 * @property reader is an interface that is responsible for NFC connection and
 * transfer of data to and from the Tangem Card.
 * Its default implementation, NfcCardReader, is in our tangem-sdk module.
 * @property cardManagerDelegate An interface that allows interaction with users and shows relevant UI.
 * Its default implementation, DefaultCardManagerDelegate, is in our tangem-sdk module.
 */
class CardManager(
        private val reader: CardReader,
        private val cardManagerDelegate: CardManagerDelegate? = null) {

    private var isBusy = false
    private val cardEnvironmentRepository = mutableMapOf<String, CardEnvironment>()
    private val cardManagerExecutor = Executors.newSingleThreadExecutor()

    init {
        CryptoUtils.initCrypto()
    }

    /**
     * To start using any card, you first need to read it using the scanCard() method.
     * This method launches an NFC session, and once it’s connected with the card,
     * it obtains the card data. Optionally, if the card contains a wallet (private and public key pair),
     * it proves that the wallet owns a private key that corresponds to a public one.
     *
     * It launches on the new thread a [ScanTask] that will send the following events in a callback:
     * [ScanEvent.OnReadEvent] after completing [com.tangem.commands.ReadCommand]
     * [ScanEvent.OnVerifyEvent] after completing [com.tangem.commands.CheckWalletCommand]
     * [TaskEvent.Completion] with an error field null after successful completion of a task or
     * [TaskEvent.Completion] with a [TaskError] if some error occurs.
     */
    fun scanCard(callback: (result: TaskEvent<ScanEvent>) -> Unit) {
        val task = ScanTask()
        runTask(task, callback = callback)
    }

    /**
     * This method allows you to sign one or multiple hashes.
     * Simultaneous signing of array of hashes in a single [SignCommand] is required to support
     * Bitcoin-type multi-input blockchains (UTXO).
     * The [SignCommand] will return a corresponding array of signatures.
     *
     * This method launches on the new thread [SignCommand] that will send the following events in a callback:
     * [SignResponse] after completing [SignCommand]
     * [TaskEvent.Completion] with an error field null after successful completion of a task or
     * [TaskEvent.Completion] with a [TaskError] if some error occurs.
     * Please note that Tangem cards usually protect the signing with a security delay
     * that may last up to 90 seconds, depending on a card.
     * It is for [CardManagerDelegate] to notify users of security delay.
     * @param hashes Array of transaction hashes. It can be from one or up to ten hashes of the same length.
     * @param cardId CID, Unique Tangem card ID number
     * @param callback
     *
     */
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

    /**

     */
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

    /**

     */
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