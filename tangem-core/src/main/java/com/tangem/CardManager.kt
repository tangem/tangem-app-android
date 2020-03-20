package com.tangem

import com.tangem.commands.*
import com.tangem.commands.personalization.CardConfig
import com.tangem.commands.personalization.DepersonalizeCommand
import com.tangem.commands.personalization.DepersonalizeResponse
import com.tangem.commands.personalization.PersonalizeCommand
import com.tangem.common.CardEnvironment
import com.tangem.common.TerminalKeysService
import com.tangem.crypto.CryptoUtils
import com.tangem.tasks.*

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
        private val cardManagerDelegate: CardManagerDelegate? = null,
        private val config: Config = Config()
) {

    private var terminalKeysService: TerminalKeysService? = null
    private var isBusy = false

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
     * @param callback is triggered on events during a performance of the task,
     * provides data in form of [ScanEvent] subclasses.
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
     * @param callback is triggered on the completion of the [SignCommand],
     * provides card response in the form of [SignResponse].
     */
    fun sign(hashes: Array<ByteArray>, cardId: String,
             callback: (result: TaskEvent<SignResponse>) -> Unit) {
        val signCommand = SignCommand(hashes)
        val task = SingleCommandTask(signCommand)
        runTask(task, cardId, callback)
    }

    /**
     * This command returns 512-byte Issuer Data field and its issuer’s signature.
     * Issuer Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use,
     * format and payload of Issuer Data. For example, this field may contain information about
     * wallet balance signed by the issuer or additional issuer’s attestation data.
     * @param cardId CID, Unique Tangem card ID number.
     * @param callback is triggered on the completion of the [ReadIssuerDataCommand],
     * provides card response in the form of [ReadIssuerDataResponse].
     */
    fun readIssuerData(cardId: String,
                       callback: (result: TaskEvent<ReadIssuerDataResponse>) -> Unit) {
        val task = ReadIssuerDataTask(config.issuerPublicKey)
        runTask(task, cardId, callback)
    }

    /**
     * This task retrieves Issuer Extra Data field and its issuer’s signature.
     * Issuer Extra Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use,
     * format and payload of Issuer Data. . For example, this field may contain photo or
     * biometric information for ID card product. Because of the large size of Issuer_Extra_Data,
     * a series of these commands have to be executed to read the entire Issuer_Extra_Data.
     * @param cardId CID, Unique Tangem card ID number.
     * @param callback is triggered on the completion of the [ReadIssuerExtraDataTask],
     * provides card response in the form of [ReadIssuerExtraDataResponse].
     */
    fun readIssuerExtraData(cardId: String,
                            callback: (result: TaskEvent<ReadIssuerExtraDataResponse>) -> Unit) {
        val task = ReadIssuerExtraDataTask(config.issuerPublicKey)
        runTask(task, cardId, callback)
    }

    /**
     * This command writes 512-byte Issuer Data field and its issuer’s signature.
     * Issuer Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use,
     * format and payload of Issuer Data. For example, this field may contain information about
     * wallet balance signed by the issuer or additional issuer’s attestation data.
     * @param cardId CID, Unique Tangem card ID number.
     * @param issuerData Data provided by issuer.
     * @param issuerDataSignature Issuer’s signature of [issuerData] with Issuer Data Private Key.
     * @param issuerDataCounter An optional counter that protect issuer data against replay attack.
     * @param callback is triggered on the completion of the [WriteIssuerDataCommand],
     * provides card response in the form of [WriteIssuerDataResponse].
     */
    fun writeIssuerData(cardId: String,
                        issuerData: ByteArray,
                        issuerDataSignature: ByteArray,
                        issuerDataCounter: Int? = null,
                        callback: (result: TaskEvent<WriteIssuerDataResponse>) -> Unit) {
        val task = WriteIssuerDataTask(
                issuerData,
                issuerDataSignature,
                issuerDataCounter,
                config.issuerPublicKey
        )
        runTask(task, cardId, callback)
    }

    /**
     * This task writes Issuer Extra Data field and its issuer’s signature.
     * Issuer Extra Data is never changed or parsed from within the Tangem COS.
     * The issuer defines purpose of use, format and payload of Issuer Data.
     * For example, this field may contain a photo or biometric information for ID card products.
     * Because of the large size of Issuer_Extra_Data, a series of these commands have to be executed
     * to write entire Issuer_Extra_Data.
     * @param cardId CID, Unique Tangem card ID number.
     * @param issuerData Data provided by issuer.
     * @param startingSignature Issuer’s signature with Issuer Data Private Key of [cardId],
     * [issuerDataCounter] (if flags Protect_Issuer_Data_Against_Replay and
     * Restrict_Overwrite_Issuer_Extra_Data are set in [SettingsMask]) and size of [issuerData].
     * @param finalizingSignature Issuer’s signature with Issuer Data Private Key of [cardId],
     * [issuerData] and [issuerDataCounter] (the latter one only if flags Protect_Issuer_Data_Against_Replay
     * andRestrict_Overwrite_Issuer_Extra_Data are set in [SettingsMask]).
     * @param issuerDataCounter An optional counter that protect issuer data against replay attack.
     * @param callback is triggered on the completion of the [WriteIssuerDataCommand],
     * provides card response in the form of [WriteIssuerDataResponse].
     */
    fun writeIssuerExtraData(cardId: String,
                             issuerData: ByteArray,
                             startingSignature: ByteArray,
                             finalizingSignature: ByteArray,
                             issuerDataCounter: Int? = null,
                             callback: (result: TaskEvent<WriteIssuerDataResponse>) -> Unit) {
        val task = WriteIssuerExtraDataTask(
                issuerData,
                startingSignature, finalizingSignature,
                config.issuerPublicKey,
                issuerDataCounter
        )
        runTask(task, cardId, callback)
    }

    /**
     * This command write some of User_Data, User_ProtectedData, User_Counter and User_ProtectedCounter fields.
     * User_Data and User_ProtectedData are never changed or parsed by the executable code the Tangem COS.
     * The App defines purpose of use, format and it's payload. For example, this field may contain cashed information
     * from blockchain to accelerate preparing new transaction.
     * User_Counter and User_ProtectedCounter are counters, that initial values can be set by App and increased on every signing
     * of new transaction (on SIGN command that calculate new signatures). The App defines purpose of use.
     * For example, this fields may contain blockchain nonce value.
     *
     * Writing of User_Counter and User_Data protected only by PIN1.
     * User_ProtectedCounter and User_ProtectedData additionaly need PIN2 to confirmation.
     */
    fun writeUserData(
            cardId: String,
            userData: ByteArray? = null,
            userProtectedData: ByteArray? = null,
            userCounter: Int? = null,
            userProtectedCounter: Int? = null,
            callback: (result: TaskEvent<WriteUserDataResponse>) -> Unit
    ) {
        val writeUserDataCommand = WriteUserDataCommand(userData, userProtectedData, userCounter, userProtectedCounter)
        val task = SingleCommandTask(writeUserDataCommand)
        runTask(task, cardId, callback)
    }

    /**
     * This command returns two up to 512-byte User_Data, User_Protected_Data and two counters User_Counter and
     * User_Protected_Counter fields.
     * User_Data and User_ProtectedData are never changed or parsed by the executable code the Tangem COS.
     * The App defines purpose of use, format and it's payload. For example, this field may contain cashed information
     * from blockchain to accelerate preparing new transaction.
     * User_Counter and User_ProtectedCounter are counters, that initial values can be set by App and increased on every signing
     * of new transaction (on SIGN command that calculate new signatures). The App defines purpose of use.
     * For example, this fields may contain blockchain nonce value.
     */
    fun readUserData(cardId: String, callback: (result: TaskEvent<ReadUserDataResponse>) -> Unit) {
        val task = SingleCommandTask(ReadUserDataCommand())
        runTask(task, cardId, callback)
    }

    /**
     * This command will create a new wallet on the card having ‘Empty’ state.
     * A key pair WalletPublicKey / WalletPrivateKey is generated and securely stored in the card.
     * App will need to obtain Wallet_PublicKey from the response of [CreateWalletCommand] or [ReadCommand]
     * and then transform it into an address of corresponding blockchain wallet
     * according to a specific blockchain algorithm.
     * WalletPrivateKey is never revealed by the card and will be used by [SignCommand] and [CheckWalletCommand].
     * RemainingSignature is set to MaxSignatures.
     * @param cardId CID, Unique Tangem card ID number.
     */
    fun createWallet(cardId: String,
                     callback: (result: TaskEvent<CreateWalletResponse>) -> Unit) {
        val createWalletCommand = CreateWalletCommand()
        val task = SingleCommandTask(createWalletCommand)
        runTask(task, cardId, callback)
    }

    /**
     * This command deletes all wallet data. If Is_Reusable flag is enabled during personalization,

     * If Is_Reusable flag is disabled, the card switches to ‘Purged’ state.
     * ‘Purged’ state is final, it makes the card useless.
     * @param cardId CID, Unique Tangem card ID number.
     */
    fun purgeWallet(cardId: String,
                    callback: (result: TaskEvent<PurgeWalletResponse>) -> Unit) {
        val purgeWalletCommand = PurgeWalletCommand()
        val task = SingleCommandTask(purgeWalletCommand)
        runTask(task, cardId, callback)
    }

    /**
     * Command available on SDK cards only
     *
     * This command resets card to initial state,
     * erasing all data written during personalization and usage.
     * @param cardId CID, Unique Tangem card ID number.
     */
    fun depersonalize(cardId: String,
                      callback: (result: TaskEvent<DepersonalizeResponse>) -> Unit) {
        val depersonalizeCommand = DepersonalizeCommand()
        val task = SingleCommandTask(depersonalizeCommand)
        runTask(task, cardId, callback)
    }

    /**
     * Command available on SDK cards only
     *
     * Personalization is an initialization procedure, required before starting using a card.
     * During this procedure a card setting is set up.
     * During this procedure all data exchange is encrypted.
     * @param config is a configuration file with all the card settings that are written on the card
     * during personalization.
     * @param cardId this parameter will set up CID, Unique Tangem card ID.
     */
    fun personalize(config: CardConfig,
                    cardId: String,
                    callback: (result: TaskEvent<Card>) -> Unit) {
        val personalizationCommand = PersonalizeCommand(config, cardId)
        val task = SingleCommandTask(personalizationCommand)
        task.performPreflightRead = false
        runTask(task, callback = callback)
    }

    /**

     */
    fun <T> runTask(task: Task<T>, cardId: String? = null, callback: (result: TaskEvent<T>) -> Unit) {
        if (isBusy) {
            callback(TaskEvent.Completion(TaskError.Busy()))
            return
        }

        val environment = prepareCardEnvironment(cardId)
        isBusy = true

        task.reader = reader
        task.delegate = cardManagerDelegate

        Thread().run {
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

    /**
     * Allows to set a particular [TerminalKeysService] to retrieve terminal keys.
     * Default implementation is provided in tangem-sdk module: [TerminalKeysStorage].
     */
    fun setTerminalKeysService(terminalKeysService: TerminalKeysService) {
        this.terminalKeysService = terminalKeysService
    }

    private fun prepareCardEnvironment(cardId: String?): CardEnvironment {
        val terminalKeys = if (config.linkedTerminal) terminalKeysService?.getKeys() else null
        return CardEnvironment(
                cardId = cardId,
                terminalKeys = terminalKeys
        )
    }

    companion object
}