package com.tangem.core.navigation.email

import java.io.File

/**
 * Email sender
 *
[REDACTED_AUTHOR]
 */
interface EmailSender {

    /** Send [email] or handle exception [onFail] */
    fun send(email: Email, onFail: ((Exception) -> Unit)? = null)

    data class Email(
        val address: String,
        val subject: String,
        val message: String,
        val attachment: File? = null,
    )
}