package com.tangem.tasks

import com.tangem.commands.*
import com.tangem.common.CardEnvironment
import com.tangem.common.CompletionResult

/**
 * This task performs [WriteIssuerExtraDataCommand] repeatedly until the issuer extra data is fully
 * written on the card.
 * @param issuerData Data provided by issuer.
 * @param startingSignature Issuer’s signature with Issuer Data Private Key of [cardId],
 * [issuerDataCounter] (if flags Protect_Issuer_Data_Against_Replay and
 * Restrict_Overwrite_Issuer_Extra_Data are set in [SettingsMask]) and size of [issuerData].
 * @param finalizingSignature Issuer’s signature with Issuer Data Private Key of [cardId],
 * [issuerData] and [issuerDataCounter] (the latter one only if flags Protect_Issuer_Data_Against_Replay
 * andRestrict_Overwrite_Issuer_Extra_Data are set in [SettingsMask]).
 * @param issuerDataCounter An optional counter that protect issuer data against replay attack.
 */
internal class WriteIssuerExtraDataTask(
        private val issuerData: ByteArray,
        private val startingSignature: ByteArray,
        private val finalizingSignature: ByteArray,
        private val issuerPublicKey: ByteArray? = null,
        private val issuerDataCounter: Int? = null
) : Task<WriteIssuerDataResponse>() {

    private lateinit var card: Card
    private lateinit var cardEnvironment: CardEnvironment

    override fun onRun(cardEnvironment: CardEnvironment,
                       currentCard: Card?,
                       callback: (result: TaskEvent<WriteIssuerDataResponse>) -> Unit) {

        card = currentCard!!
        this.cardEnvironment = cardEnvironment
        val command = WriteIssuerExtraDataCommand(
                issuerData, startingSignature, finalizingSignature, issuerDataCounter
        )
        if (!isCounterValid(issuerDataCounter)) {
            completeNfcSession(TaskError.MissingCounter())
            callback(TaskEvent.Completion(TaskError.MissingCounter()))
        } else if (!verifySignatures(command)) {
            completeNfcSession(TaskError.VerificationFailed())
            callback(TaskEvent.Completion(TaskError.VerificationFailed()))
        }

        writeIssuerData(command, callback)
    }

    private fun writeIssuerData(
            command: WriteIssuerExtraDataCommand,
            callback: (result: TaskEvent<WriteIssuerDataResponse>) -> Unit) {

        if (command.mode == IssuerDataMode.WriteExtraData) {
            delegate?.onDelay(issuerData.size, command.offset, WriteIssuerExtraDataCommand.SINGLE_WRITE_SIZE)
        }
        sendCommand(command, cardEnvironment) { result ->
            when (result) {

                is CompletionResult.Success -> {
                    when (command.mode) {
                        IssuerDataMode.InitializeWritingExtraData -> {
                            command.mode = IssuerDataMode.WriteExtraData
                            writeIssuerData(command, callback)
                            return@sendCommand
                        }
                        IssuerDataMode.WriteExtraData -> {
                            command.offset += WriteIssuerExtraDataCommand.SINGLE_WRITE_SIZE
                            if (command.offset >= issuerData.size) {
                                command.mode = IssuerDataMode.FinalizeExtraData
                            }
                            writeIssuerData(command, callback)
                            return@sendCommand
                        }
                        IssuerDataMode.FinalizeExtraData -> {
                            completeNfcSession()
                            callback(TaskEvent.Event(result.data))
                            callback(TaskEvent.Completion())
                        }
                    }
                }
                is CompletionResult.Failure -> {
                    if (result.error !is TaskError.UserCancelled) {
                        completeNfcSession(result.error)
                    }
                    callback(TaskEvent.Completion(result.error))
                }
            }
        }
    }

    private fun isCounterValid(issuerDataCounter: Int?): Boolean =
            if (isCounterRequired()) issuerDataCounter != null else true

    private fun isCounterRequired(): Boolean =
            card.settingsMask?.contains(SettingsMask.protectIssuerDataAgainstReplay) != false

    private fun verifySignatures(command: WriteIssuerExtraDataCommand): Boolean {
        val publicKey = issuerPublicKey ?: card.issuerPublicKey!!
        val cardId = cardEnvironment.cardId!!

        val firstData = IssuerDataToVerify(cardId, null, issuerDataCounter, issuerData.size)
        val secondData = IssuerDataToVerify(cardId, issuerData, issuerDataCounter)

        return command.verify(publicKey, startingSignature, firstData) &&
                command.verify(publicKey, finalizingSignature, secondData)
    }
}