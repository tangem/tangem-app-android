package com.tangem.common.module

/**
[REDACTED_AUTHOR]
 * The base object for communication between modules
 */
@Deprecated("Will be removed")
interface ModuleMessage

/**
 * @property code describes what feature is the error coming from
 * @property message the error description
 * @property data any data that can help in the part where this error is being handled
 */
@Deprecated("Will be removed")
abstract class ModuleError : Throwable(), ModuleMessage {
    abstract val code: Int
    abstract override val message: String
    abstract val data: Any?
}

@Deprecated("Will be removed")
interface ModuleMessageConverter<ModuleMessage, R> {
    fun convert(message: ModuleMessage): R
}