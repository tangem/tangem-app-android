package com.tangem

import com.tangem.common.extensions.CardType
import java.util.*

class Config(
        /**
        * Enables or disables Linked Terminal feature.

        App can optionally generate ECDSA key pair Terminal_PrivateKey / Terminal_PublicKey.
        And then submit Terminal_PublicKey to the card in any SIGN command.
        Once SIGN is successfully executed by COS (Card Operation System),
        including PIN2 verification and/or completion of security delay, the submitted
        Terminal_PublicKey key is stored by COS. After that, the App instance is deemed trusted
        by COS and COS will allow skipping security delay for subsequent SIGN operations
        thus improving convenience without sacrificing security.

        In order to skip security delay, App should use Terminal_PrivateKey to compute the signature
        of the data being submitted to SIGN command for signing and transmit this signature in
        Terminal_Transaction_Signature parameter in the same SIGN command. COS will verify
        the correctness of Terminal_Transaction_Signature using previously stored Terminal_PublicKey
        and, if correct, will skip security delay for the current SIGN operation.
         */
        var linkedTerminal: Boolean = true,

        /**
         * If not null, it will be used to validate Issuer data and issuer extra data.
         * If null, issuerPublicKey from current card will be used.
         */
        var issuerPublicKey: ByteArray? = null,

        /**
         * Level of encryption used in communication with a Tangem Card.
         */
        var encryptionMode: EncryptionMode = EncryptionMode.NONE,

        /**
         * Type of cards that are allowed to be interacted with in TangemSdk.
         */
        var allowedCards: EnumSet<CardType> = EnumSet.allOf(CardType::class.java)
)