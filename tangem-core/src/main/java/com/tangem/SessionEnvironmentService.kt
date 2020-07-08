package com.tangem

import com.tangem.common.CardValuesStorage
import com.tangem.common.PinCode
import com.tangem.common.TerminalKeysService

class SessionEnvironmentService(
        private val config: Config,
        private val terminalKeysService: TerminalKeysService?,
        private val cardValuesStorage: CardValuesStorage?
) {

    fun createEnvironment(cardId: String?): SessionEnvironment {
        val terminalKeys = if (config.linkedTerminal) terminalKeysService?.getKeys() else null

        val cardValues = cardId?.let { cardValuesStorage?.getValues(cardId) }

        val cardVerification = cardValues?.cardVerification ?: VerificationState.NotVerified
        val cardValidation = cardValues?.cardValidation ?: VerificationState.NotVerified
        val codeVerification = cardValues?.codeVerification ?: VerificationState.NotVerified

        val pin1 = TangemSdk.pin1
                ?: if (cardValues?.isPin1Default != false) {
                    PinCode(config.defaultPin1, true)
                } else {
                    null
                }

        val pin2 = cardId?.let { TangemSdk.pin2[it] }
                ?: if (cardValues?.isPin2Default != false) {
                    PinCode(config.defaultPin2, true)
                } else {
                    null
                }

        return SessionEnvironment(
                terminalKeys = terminalKeys,
                cardFilter = config.cardFilter,
                handleErrors = config.handleErrors,

                encryptionMode = config.encryptionMode,

                cardVerification = cardVerification,
                cardValidation = cardValidation,
                codeVerification = codeVerification,

                pin1 = pin1,
                pin2 = pin2
        )
    }

    fun updateEnvironment(environment: SessionEnvironment, cardId: String) {
        val cardValues = cardId.let { cardValuesStorage?.getValues(it) }
        environment.cardVerification = cardValues?.cardVerification ?: VerificationState.NotVerified
        environment.cardValidation = cardValues?.cardValidation ?: VerificationState.NotVerified
        environment.codeVerification = cardValues?.codeVerification ?: VerificationState.NotVerified

        if (cardValues?.isPin1Default == false && environment.pin1?.isDefault == true) environment.pin1 = null
        if (cardValues?.isPin2Default == false && environment.pin1?.isDefault == true) environment.pin2 = null
    }

    fun saveEnvironmentValues(environment: SessionEnvironment, cardId: String?) {
        if (config.savePin1InStaticField) {
            TangemSdk.pin1 = environment.pin1
        }
        if (config.savePin2InStaticField) {
            cardId?.let { cardId -> TangemSdk.pin2[cardId] = environment.pin2 }
        }
        cardId?.let { cardId ->
            cardValuesStorage?.saveValues(cardId,
                    environment.pin1?.isDefault ?: false,
                    environment.pin2?.isDefault ?: false,
                    environment.cardVerification, environment.cardVerification,
                    environment.codeVerification)
        }
    }
}