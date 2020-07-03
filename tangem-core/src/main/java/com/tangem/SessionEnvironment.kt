package com.tangem

import com.tangem.commands.Card
import com.tangem.commands.EllipticCurve
import com.tangem.common.CardValuesStorage
import com.tangem.common.PinCode
import com.tangem.common.TerminalKeysService
import com.tangem.crypto.CryptoUtils.generatePublicKey


/**
 * Contains data relating to a Tangem card. It is used in constructing all the commands,
 * and commands can return modified [SessionEnvironment].
 *
 * @param cardId  Card ID, if it is known before tapping the card.
 * @property config sets a number of parameters for communication with Tangem cards.
 * @param terminalKeysService is used to retrieve terminal keys used in Linked Terminal feature.
 * @param cardValuesStorage is used to save and retrieve some values relating to a particular card.
 *
 * @property pin1 An access Code, required to get access to a card. A default value is set in [Config]
 * @property pin2 A code, required to perform particular operations with a card. A default value is set in [Config]
 * @property terminalKeys generated terminal keys used in Linked Terminal feature
 * @property cardFilter a property that defines types of card that this SDK will be able to interact with
 * @property handleErrors if true, the SDK parses internal card errors into concrete [TangemSdkError]
 * @property encryptionMode preferred [EncryptionMode] for interaction with cards
 * @property encryptionKey is used for encrypted communication with a card
 *
 */
class SessionEnvironment(
        cardId: String?,
        private val config: Config,
        terminalKeysService: TerminalKeysService?,
        private val cardValuesStorage: CardValuesStorage?
) {

    var pin1: PinCode?
    var pin2: PinCode?
    var cvc: ByteArray? = null

    var terminalKeys: KeyPair? = null
    var cardFilter: CardFilter
    val handleErrors: Boolean

    var encryptionMode: EncryptionMode
    var encryptionKey: ByteArray? = null

    var cardVerification: VerificationState
    var cardValidation: VerificationState
    var codeVerification: VerificationState

    var card: Card? = null

    init {
        terminalKeys = if (config.linkedTerminal) terminalKeysService?.getKeys() else null
        cardFilter = config.cardFilter
        handleErrors = config.handleErrors

        encryptionMode = config.encryptionMode

        val cardValues = cardId?.let { cardValuesStorage?.getValues(cardId) }

        cardVerification = cardValues?.cardVerification ?: VerificationState.NotVerified
        cardValidation = cardValues?.cardValidation ?: VerificationState.NotVerified
        codeVerification = cardValues?.codeVerification ?: VerificationState.NotVerified

        pin1 = TangemSdk.pin1
                ?: if (cardValues?.isPin1Default != false) {
                    PinCode(config.defaultPin1, true)
                } else {
                    null
                }

        pin2 = cardId?.let { TangemSdk.pin2[it] }
                ?: if (cardValues?.isPin2Default != false) {
                    PinCode(config.defaultPin2, true)
                } else {
                    null
                }
    }

    fun restoreCardValues() {
        val cardValues = this.card?.cardId?.let { cardValuesStorage?.getValues(it) }
        cardVerification = cardValues?.cardVerification ?: VerificationState.NotVerified
        cardValidation = cardValues?.cardValidation ?: VerificationState.NotVerified
        codeVerification = cardValues?.codeVerification ?: VerificationState.NotVerified

        if (cardValues?.isPin1Default == false && pin1?.isDefault == true) pin1 = null
        if (cardValues?.isPin2Default == false && pin1?.isDefault == true) pin2 = null
    }

    fun saveCardValues() {
        if (config.savePin1InStaticField) {
            TangemSdk.pin1 = pin1
        }
        if (config.savePin2InStaticField) {
            card?.cardId?.let { cardId -> TangemSdk.pin2[cardId] = pin2 }
        }

        cardValuesStorage?.saveValues(this)
    }

}

/**
 * All possible encryption modes.
 */
enum class EncryptionMode(val code: Int) {
    NONE(0x0),
    FAST(0x1),
    STRONG(0x2)
}

class KeyPair(val publicKey: ByteArray, val privateKey: ByteArray) {

    constructor(privateKey: ByteArray, curve: EllipticCurve = EllipticCurve.Secp256k1) :
            this(generatePublicKey(privateKey, curve), privateKey)
}

enum class VerificationState {
    Passed, Offline, Failed, NotVerified, Cancelled
}