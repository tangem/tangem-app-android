package com.tangem.common.module

/**
 * Created by Anton Zhilenkov on 14/04/2022.
 * The base object for communication between modules
 */
interface ModuleMessage

interface ModuleMessageConverter<ModuleMessage, R> {
    fun convert(message: ModuleMessage): R
}

/**
 * @property code describes what feature is the error coming from
 * @property message the error description
 * @property data any data that can help in the part where this error is being handled
 */
interface ModuleError : ModuleMessage {
    val code: Int
    val message: String
    val data: Any?
}
