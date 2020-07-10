package com.tangem

import com.tangem.commands.Card
import com.tangem.commands.EllipticCurve
import com.tangem.common.PinCode
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
        var pin1: PinCode? = PinCode(Config.DEFAULT_PIN_1, true),
        var pin2: PinCode? = PinCode(Config.DEFAULT_PIN_2, true),
        var cvc: ByteArray? = null,

        var terminalKeys: KeyPair? = null,
        var cardFilter: CardFilter = CardFilter(),
        val handleErrors: Boolean = true,

        var encryptionMode: EncryptionMode = EncryptionMode.NONE,
        var encryptionKey: ByteArray? = null,

        var cardVerification: VerificationState = VerificationState.NotVerified,
        var cardValidation: VerificationState = VerificationState.NotVerified,
        var codeVerification: VerificationState = VerificationState.NotVerified,

        var card: Card? = null
)

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
