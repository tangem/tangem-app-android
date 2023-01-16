package com.tangem.common.module

/**
 * Created by Anton Zhilenkov on 14/04/2022.
 * The base object for communication between modules
 */
interface ModuleMessage

/**
 * @property code describes what feature is the error coming from
 * @property message the error description
 * @property data any data that can help in the part where this error is being handled
 */
abstract class ModuleError : Throwable(), ModuleMessage {
    abstract val code: Int
    abstract override val message: String
    abstract val data: Any?
}

/**
 * An exception marked as FbConsumeException should be submitted to Firebase.Crashlytics as a non-fatal issue.
 */
interface FbConsumeException

interface ModuleMessageConverter<ModuleMessage, R> {
    fun convert(message: ModuleMessage): R
}
