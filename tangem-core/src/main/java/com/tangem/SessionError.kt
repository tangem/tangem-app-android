package com.tangem

import com.tangem.commands.Card
import com.tangem.commands.ReadCommand
import com.tangem.common.apdu.StatusWord
import com.tangem.tasks.ScanTask

/**
 * An error class that represent typical errors that may occur when performing Tangem SDK tasks.
 * Errors are propagated back to the caller in callbacks.
 */
sealed class SessionError(val code: Int) : Exception() {

    //Errors in serializing APDU
    /**
     * This error is returned when there [CommandSerializer] cannot deserialize [com.tangem.common.tlv.Tlv]
     * (this error is a wrapper around internal [com.tangem.common.tlv.TlvDecoder] errors).
     */
    class SerializeCommandError : SessionError(1000)

    class DeserializeApduFailed : SessionError(1001)
    class EncodingFailedTypeMismatch : SessionError(1002)
    class EncodingFailed : SessionError(1003)

    class DecodingFailedMissingTag : SessionError(1004)
    class DecodingFailedTypeMismatch : SessionError(1005)
    class DecodingFailed : SessionError(1005)

    /**
     * This error is returned when unknown [StatusWord] is received from a card.
     */
    class UnknownStatus : SessionError(2001)

    /**
     * This error is returned when a card's reply is [StatusWord.ErrorProcessingCommand].
     * The card sends this status in case of internal card error.
     */
    class ErrorProcessingCommand : SessionError(2002)

    /**
     * This error is returned when a task (such as [ScanTask]) requires that [ReadCommand]
     * is executed before performing other commands.
     */
    class MissingPreflightRead : SessionError(2003)

    /**
     * This error is returned when a card's reply is [StatusWord.InvalidState].
     * The card sends this status when command can not be executed in the current state of a card.
     */
    class InvalidState : SessionError(2004)

    /**
     * This error is returned when a card's reply is [StatusWord.InsNotSupported].
     * The card sends this status when the card cannot process the [com.tangem.common.apdu.Instruction].
     */
    class InsNotSupported : SessionError(2005)

    /**
     * This error is returned when a card's reply is [StatusWord.InvalidParams].
     * The card sends this status when there are wrong or not sufficient parameters in TLV request,
     * or wrong PIN1/PIN2.
     * The error may be caused, for example, by wrong parameters of the [Task], [CommandSerializer],
     * mapping or serialization errors.
     */
    class InvalidParams : SessionError(2006)

    /**
     * This error is returned when a card's reply is [StatusWord.NeedEncryption]
     * and the encryption was not established by TangemSdk.
     */
    class NeedEncryption : SessionError(2007)

    //Scan errors
    /**
     * This error is returned when a [Task] checks unsuccessfully either
     * a card's ability to sign with its private key, or the validity of issuer data.
     */
    class VerificationFailed : SessionError(3000)

    /**
     * This error is returned when a [ScanTask] returns a [Card] without some of the essential fields.
     */
    class CardError : SessionError(3001)

    /**
     * This error is returned when a [Task] expects a user to use a particular card,
     * and a user tries to use a different card.
     */
    class WrongCard : SessionError(3002)

    /**
     * Tangem cards can sign currently up to 10 hashes during one [com.tangem.commands.SignCommand].
     * This error is returned when a [com.tangem.commands.SignCommand] receives more than 10 hashes to sign.
     */
    class TooMuchHashesInOneTransaction : SessionError(3003)

    /**
     * This error is returned when a [com.tangem.commands.SignCommand]
     * receives only empty hashes for signature.
     */
    class EmptyHashes : SessionError(3004)

    /**
     * This error is returned when a [com.tangem.commands.SignCommand]
     * receives hashes of different lengths for signature.
     */
    class HashSizeMustBeEqual : SessionError(3005)

    /**
     * This error is returned when [com.tangem.TangemSdk] was called with a new [Task],
     * while a previous [Task] is still in progress.
     */
    class Busy : SessionError(4000)

    /**
     * This error is returned when a user manually closes NFC Reading Bottom Sheet Dialog.
     */
    class UserCancelled : SessionError(4001)

    //NFC errors
    class NfcReaderError : SessionError(5002)

    /**
     * This error is returned when Android  NFC reader loses a tag
     * (e.g. a user detaches card from the phone's NFC module) while the NFC session is in progress.
     */
    class TagLost : SessionError(5003)

    class UnknownError : SessionError(6000)

    //Specific Command Errors
    /**
     * This error is returned when [ReadIssuerDataTask] or [ReadIssuerExtraDataTask] expects a counter
     * (when the card's requires it), but the counter is missing.
     */
    class MissingCounter : SessionError(7001)

    class MissingIssuerPubicKey : SessionError(7002)
}