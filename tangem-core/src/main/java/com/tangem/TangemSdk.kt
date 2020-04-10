package com.tangem

import com.tangem.commands.*
import com.tangem.commands.personalization.DepersonalizeCommand
import com.tangem.commands.personalization.DepersonalizeResponse
import com.tangem.commands.personalization.PersonalizeCommand
import com.tangem.commands.personalization.entities.Acquirer
import com.tangem.commands.personalization.entities.CardConfig
import com.tangem.commands.personalization.entities.Issuer
import com.tangem.commands.personalization.entities.Manufacturer
import com.tangem.common.CompletionResult
import com.tangem.common.TerminalKeysService
import com.tangem.crypto.CryptoUtils
import com.tangem.tasks.CreateWalletTask
import com.tangem.tasks.ScanTask

/**
 * The main interface of Tangem SDK that allows your app to communicate with Tangem cards.
 *
 * @property reader is an interface that is responsible for NFC connection and
 * transfer of data to and from the Tangem Card.
 * Its default implementation, NfcCardReader, is in our tangem-sdk module.
 * @property viewDelegate An interface that allows interaction with users and shows relevant UI.
 * Its default implementation, DefaultCardSessionViewDelegate, is in our tangem-sdk module.
 * @property config allows to change a number of parameters for communication with Tangem cards.
 * Do not change the default values unless you know what you are doing.
 */
class TangemSdk(
        private val reader: CardReader,
        private val viewDelegate: CardSessionViewDelegate,
        var config: Config = Config()
) {

    private var terminalKeysService: TerminalKeysService? = null
    private var cardSession: CardSession? = null

    init {
        CryptoUtils.initCrypto()
    }

    /**
     * This method launches a [ScanTask] on a new thread.
     *
     * To start using any card, you first need to read it using the scanCard() method.
     * This method launches an NFC session, and once it’s connected with the card,
     * it obtains the card data. Optionally, if the card contains a wallet (private and public key pair),
     * it proves that the wallet owns a private key that corresponds to a public one.
     *
     * @param callback is triggered on the completion of the [ScanTask] and provides card response
     * in the form of [Card] if the task was performed successfully or [SessionError] in case of an error.
     */
    fun scanCard(callback: (result: CompletionResult<Card>) -> Unit) {
        cardSession = CardSession(buildEnvironment(), reader, viewDelegate)
        cardSession!!.startWithRunnable(ScanTask(), callback)
    }

    /**
     * This method launches a [SignCommand] on a new thread.
     *
     * It allows you to sign one or multiple hashes.
     * Simultaneous signing of array of hashes in a single [SignCommand] is required to support
     * Bitcoin-type multi-input blockchains (UTXO).
     * The [SignCommand] will return a corresponding array of signatures.
     *
     * Please note that Tangem cards usually protect the signing with a security delay
     * that may last up to 90 seconds, depending on a card.
     * It is for [CardSessionViewDelegate] to notify users of security delay.
     *
     * @param hashes Array of transaction hashes. It can be from one or up to ten hashes of the same length.
     * @param cardId CID, Unique Tangem card ID number
     * @param callback is triggered on the completion of the [SignCommand] and provides card response
     * in the form of [SignResponse] if the task was performed successfully
     * or [SessionError] in case of an error.
     */
    fun sign(hashes: Array<ByteArray>, cardId: String,
             callback: (result: CompletionResult<SignResponse>) -> Unit) {
        cardSession = CardSession(buildEnvironment(), reader, viewDelegate, cardId)
        cardSession!!.startWithRunnable(SignCommand(hashes), callback)
    }

    /**
     * This method launches a [ReadIssuerDataCommand] on a new thread.

     * This command returns 512-byte Issuer Data field and its issuer’s signature.
     * Issuer Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use,
     * format and payload of Issuer Data. For example, this field may contain information about
     * wallet balance signed by the issuer or additional issuer’s attestation data.
     *
     * @param cardId CID, Unique Tangem card ID number.
     * @param callback is triggered on the completion of the [ReadIssuerDataCommand] and provides
     * card response in the form of [ReadIssuerDataResponse] if the task was performed successfully
     * or [SessionError] in case of an error.
     */
    fun readIssuerData(cardId: String,
                       callback: (result: CompletionResult<ReadIssuerDataResponse>) -> Unit) {
        cardSession = CardSession(buildEnvironment(), reader, viewDelegate, cardId)
        cardSession!!.startWithRunnable(ReadIssuerDataCommand(config.issuerPublicKey), callback)
    }

    /**
     * This method launches a [ReadIssuerExtraDataCommand] on a new thread.
     *
     * This command retrieves Issuer Extra Data field and its issuer’s signature.
     * Issuer Extra Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use,
     * format and payload of Issuer Data. . For example, this field may contain photo or
     * biometric information for ID card product. Because of the large size of Issuer_Extra_Data,
     * a series of these commands have to be executed to read the entire Issuer_Extra_Data.
     *
     * @param cardId CID, Unique Tangem card ID number.
     * @param callback is triggered on the completion of the [ReadIssuerExtraDataCommand] and provides
     * card response in the form of [ReadIssuerExtraDataResponse] if the task was performed successfully
     * or [SessionError] in case of an error.
     */
    fun readIssuerExtraData(cardId: String,
                            callback: (result: CompletionResult<ReadIssuerExtraDataResponse>) -> Unit) {
        cardSession = CardSession(buildEnvironment(), reader, viewDelegate, cardId)
        cardSession!!.startWithRunnable(ReadIssuerExtraDataCommand(config.issuerPublicKey), callback)
    }

    /**
     * This method launches a [WriteIssuerDataCommand] on a new thread.
     *
     * This command writes 512-byte Issuer Data field and its issuer’s signature.
     * Issuer Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use,
     * format and payload of Issuer Data. For example, this field may contain information about
     * wallet balance signed by the issuer or additional issuer’s attestation data.
     *
     * @param cardId CID, Unique Tangem card ID number.
     * @param issuerData Data provided by issuer.
     * @param issuerDataSignature Issuer’s signature of [issuerData] with Issuer Data Private Key.
     * @param issuerDataCounter An optional counter that protect issuer data against replay attack.
     * @param callback is triggered on the completion of the [WriteIssuerDataCommand] and provides
     * card response in the form of [WriteIssuerDataResponse] if the task was performed successfully
     * or [SessionError] in case of an error.
     */
    fun writeIssuerData(cardId: String,
                        issuerData: ByteArray,
                        issuerDataSignature: ByteArray,
                        issuerDataCounter: Int? = null,
                        callback: (result: CompletionResult<WriteIssuerDataResponse>) -> Unit) {
        val command = WriteIssuerDataCommand(
                issuerData,
                issuerDataSignature,
                issuerDataCounter,
                config.issuerPublicKey
        )
        cardSession = CardSession(buildEnvironment(), reader, viewDelegate, cardId)
        cardSession!!.startWithRunnable(command, callback)
    }

    /**
     * This method launches a [WriteIssuerExtraDataCommand] on a new thread.
     *
     * This command writes Issuer Extra Data field and its issuer’s signature.
     * Issuer Extra Data is never changed or parsed from within the Tangem COS.
     * The issuer defines purpose of use, format and payload of Issuer Data.
     * For example, this field may contain a photo or biometric information for ID card products.
     * Because of the large size of IssuerExtraData, a series of these commands have to be executed
     * to write entire IssuerExtraData.
     *
     * @param cardId CID, Unique Tangem card ID number.
     * @param issuerData Data provided by issuer.
     * @param startingSignature Issuer’s signature with Issuer Data Private Key of [cardId],
     * [issuerDataCounter] (if flags Protect_Issuer_Data_Against_Replay and
     * Restrict_Overwrite_Issuer_Extra_Data are set in [SettingsMask]) and size of [issuerData].
     * @param finalizingSignature Issuer’s signature with Issuer Data Private Key of [cardId],
     * [issuerData] and [issuerDataCounter] (the latter one only if flags Protect_Issuer_Data_Against_Replay
     * andRestrict_Overwrite_Issuer_Extra_Data are set in [SettingsMask]).
     * @param issuerDataCounter An optional counter that protect issuer data against replay attack.
     * @param callback is triggered on the completion of the [WriteIssuerExtraDataCommand] and provides
     * card response in the form of [WriteIssuerDataResponse] if the task was performed successfully
     * or [SessionError] in case of an error.
     */
    fun writeIssuerExtraData(cardId: String,
                             issuerData: ByteArray,
                             startingSignature: ByteArray,
                             finalizingSignature: ByteArray,
                             issuerDataCounter: Int? = null,
                             callback: (result: CompletionResult<WriteIssuerDataResponse>) -> Unit) {
        val command = WriteIssuerExtraDataCommand(
                issuerData,
                startingSignature, finalizingSignature,
                issuerDataCounter,
                config.issuerPublicKey
        )
        cardSession = CardSession(buildEnvironment(), reader, viewDelegate, cardId)
        cardSession!!.startWithRunnable(command, callback)
    }

    /**
     * This method launches a [WriteUserDataCommand] on a new thread.
     *
     * This command writes some of UserData, UserProtectedData, UserCounter and UserProtectedCounter fields.
     * User_Data and User_ProtectedData are never changed or parsed by the executable code the Tangem COS.
     * The App defines purpose of use, format and it's payload. For example, this field may contain cashed information
     * from blockchain to accelerate preparing new transaction.
     * User_Counter and User_ProtectedCounter are counters, that initial values can be set by App and increased on every signing
     * of new transaction (on SIGN command that calculate new signatures). The App defines purpose of use.
     * For example, this fields may contain blockchain nonce value.
     *
     * Writing of UserCounter and UserData is protected only by PIN1.
     * UserProtectedCounter and UserProtectedData need additionally PIN2 to confirmation.
     */
    fun writeUserData(
            cardId: String,
            userData: ByteArray? = null,
            userProtectedData: ByteArray? = null,
            userCounter: Int? = null,
            userProtectedCounter: Int? = null,
            callback: (result: CompletionResult<WriteUserDataResponse>) -> Unit
    ) {
        val command = WriteUserDataCommand(userData, userProtectedData, userCounter, userProtectedCounter)
        cardSession = CardSession(buildEnvironment(), reader, viewDelegate, cardId)
        cardSession!!.startWithRunnable(command, callback)
    }

    /**
     * This method launches a [ReadUserDataCommand] on a new thread.
     *
     * This command returns two up to 512-byte User_Data, User_Protected_Data and two counters User_Counter and
     * User_Protected_Counter fields.
     * User_Data and User_ProtectedData are never changed or parsed by the executable code the Tangem COS.
     * The App defines purpose of use, format and it's payload. For example, this field may contain cashed information
     * from blockchain to accelerate preparing new transaction.
     * User_Counter and User_ProtectedCounter are counters, that initial values can be set by App and increased on every signing
     * of new transaction (on SIGN command that calculate new signatures). The App defines purpose of use.
     * For example, this fields may contain blockchain nonce value.
     *
     * @param cardId CID, Unique Tangem card ID number.
     * @param callback is triggered on the completion of the [ReadUserDataCommand] and provides
     * card response in the form of [ReadUserDataResponse] if the task was performed successfully
     * or [SessionError] in case of an error.
     */
    fun readUserData(cardId: String, callback: (result: CompletionResult<ReadUserDataResponse>) -> Unit) {
        cardSession = CardSession(buildEnvironment(), reader, viewDelegate, cardId)
        cardSession!!.startWithRunnable(ReadUserDataCommand(), callback)
    }

    /**
     * This method launches a [CreateWalletTask] on a new thread.
     *
     * This this will create a new wallet on the card having ‘Empty’ state with [CreateWalletCommand]
     * and will check the success of the operation by performing [CheckWalletCommand].
     * A key pair WalletPublicKey / WalletPrivateKey is generated and securely stored in the card.
     * App will need to obtain Wallet_PublicKey from the [CreateWalletResponse] or from the
     * response of [ReadCommand] and then transform it into an address of corresponding
     * blockchain wallet according to a specific blockchain algorithm.
     * WalletPrivateKey is never revealed by the card and will be used by [SignCommand] and [CheckWalletCommand].
     * RemainingSignature is set to MaxSignatures.
     *
     * @param cardId CID, Unique Tangem card ID number.
     * @param callback is triggered on the completion of the [CreateWalletTask] and provides
     * card response in the form of [CreateWalletResponse] if the task was performed successfully
     * or [SessionError] in case of an error.
     */
    fun createWallet(cardId: String,
                     callback: (result: CompletionResult<CreateWalletResponse>) -> Unit) {
        cardSession = CardSession(buildEnvironment(), reader, viewDelegate, cardId)
        cardSession!!.startWithRunnable(CreateWalletTask(), callback)
    }

    /**
     * This method launches a [PurgeWalletCommand] on a new thread.
     *
     * This command deletes all wallet data. If IsReusable flag is enabled during personalization,

     * or [CreateWalletCommand].
     * If IsReusable flag is disabled, the card switches to ‘Purged’ state.
     * ‘Purged’ state is final, it makes the card useless.
     *
     * @param cardId CID, Unique Tangem card ID number.
     * @param callback is triggered on the completion of the [PurgeWalletCommand] and provides
     * card response in the form of [PurgeWalletResponse] if the task was performed successfully
     * or [SessionError] in case of an error.
     */
    fun purgeWallet(cardId: String,
                    callback: (result: CompletionResult<PurgeWalletResponse>) -> Unit) {
        cardSession = CardSession(buildEnvironment(), reader, viewDelegate, cardId)
        cardSession!!.startWithRunnable(PurgeWalletCommand(), callback)
    }

    /**
     * Command available on SDK cards only
     *
     * This method launches a [DepersonalizeCommand] on a new thread.
     *
     * This command resets card to initial state,
     * erasing all data written during personalization and usage.
     *
     * @param cardId CID, Unique Tangem card ID number.
     * @param callback is triggered on the completion of the [DepersonalizeCommand] and provides
     * card response in the form of [DepersonalizeResponse] if the task was performed successfully
     * or [SessionError] in case of an error.
     * */
    fun depersonalize(cardId: String,
                      callback: (result: CompletionResult<DepersonalizeResponse>) -> Unit) {
        cardSession = CardSession(buildEnvironment(), reader, viewDelegate, cardId)
        cardSession!!.startWithRunnable(DepersonalizeCommand(), callback)
    }

    /**
     * Command available on SDK cards only
     *
     * This method launches a [PersonalizeCommand] on a new thread.
     *
     * Personalization is an initialization procedure, required before starting using a card.
     * During this procedure a card setting is set up.
     * During this procedure all data exchange is encrypted.
     * @param config is a configuration file with all the card settings that are written on the card
     * during personalization.
     * @param issuer Issuer is a third-party team or company wishing to use Tangem cards.
     * @param manufacturer Tangem Card Manufacturer.
     * @param acquirer Acquirer is a trusted third-party company that operates proprietary
     * (non-EMV) POS terminal infrastructure and transaction processing back-end.
     * @param callback is triggered on the completion of the [PersonalizeCommand] and provides
     * card response in the form of [Card] if the command was performed successfully
     * or [SessionError] in case of an error.
     */
    fun personalize(config: CardConfig,
                    issuer: Issuer, manufacturer: Manufacturer, acquirer: Acquirer? = null,
                    callback: (result: CompletionResult<Card>) -> Unit) {
        val command = PersonalizeCommand(config, issuer, manufacturer, acquirer)
        cardSession = CardSession(buildEnvironment(), reader, viewDelegate)
        cardSession!!.startWithRunnable(command, callback)
    }

    /**
     * Allows running a custom bunch of commands in one [CardSession] by creating a custom task.
     * [TangemSdk] will start a card session, perform preflight [ReadCommand],
     * invoke [CardSessionRunnable.run] and close the session.
     * You can find the current card in the [CardSession.environment].

     * @runnable: A custom task, adopting [CardSessionRunnable] protocol
     * @cardId: CID, Unique Tangem card ID number. If not null, the SDK will check that you the card
     * with which you tapped a phone has this [cardId] and SDK will return
     * the [SessionError.WrongCard] otherwise.
     * @initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     *  @callback: Standard [TangemSdk] callback.
     */
    fun <T : CommandResponse> startSession(
            runnable: CardSessionRunnable<T>, cardId: String? = null, initialMessage: Message? = null,
            callback: (result: CompletionResult<T>) -> Unit) {
        cardSession = CardSession(buildEnvironment(), reader, viewDelegate, cardId, initialMessage)
        Thread().run { cardSession!!.startWithRunnable(runnable, callback) }
    }

    /**
     * Allows running  a custom bunch of commands in one [CardSession] with lightweight closure syntax.
     * Tangem SDK will start a card sesion and perform preflight [ReadCommand].

     * @cardId: CID, Unique Tangem card ID number. If not null, the SDK will check that you the card
     * with which you tapped a phone has this [cardId] and SDK will return
     * the [SessionError.WrongCard] otherwise.
     * @initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @callback: At first, you should check that the [SessionError] is not null,
     * then you can use the [CardSession] to interact with a card.
     */
    fun <T : CommandResponse> startSession(
            cardId: String? = null, initialMessage: Message? = null,
            callback: (session: CardSession, error: SessionError?) -> Unit) {
        cardSession = CardSession(buildEnvironment(), reader, viewDelegate, cardId, initialMessage)
        Thread().run { cardSession!!.start(callback) }
    }

    /**
     * Allows to set a particular [TerminalKeysService] to retrieve terminal keys.
     * Default implementation is provided in tangem-sdk module: [TerminalKeysStorage].
     */
    fun setTerminalKeysService(terminalKeysService: TerminalKeysService) {
        this.terminalKeysService = terminalKeysService
    }

    private fun buildEnvironment(): CardEnvironment {
        val terminalKeys = if (config.linkedTerminal) terminalKeysService?.getKeys() else null
        return CardEnvironment(
                terminalKeys = terminalKeys
        )
    }

    companion object
}