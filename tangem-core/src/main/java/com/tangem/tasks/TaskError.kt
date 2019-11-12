package com.tangem.tasks

/**
 * An error class that covers typical errors that may occur when performing Tangem SDK tasks.
 * Errors are got propagated back within callbacks.
 */
sealed class TaskError(description: String? = null) : Exception(description) {
    class UnknownStatus(sw: Int) : TaskError()
    class MappingError : TaskError()
    class GenericError(description: String? = null) : TaskError(description)
    class UserCancelledError() : TaskError()
    class Busy() : TaskError()

    class ErrorProcessingCommand : TaskError()
    class InvalidState : TaskError()
    class InsNotSupported : TaskError()
    class InvalidParams : TaskError()
    class NeedEncryption : TaskError()
    class NeedPause : TaskError()

    class VefificationFailed : TaskError()
    class CardError : TaskError()
    class ReaderError() : TaskError()
    class SerializeCommandError() : TaskError()

    class CardIsMissing() : TaskError()
    class EmptyHashes() : TaskError()
    class TooMuchHashes() : TaskError()
    class HashSizeMustBeEqual() : TaskError()
}