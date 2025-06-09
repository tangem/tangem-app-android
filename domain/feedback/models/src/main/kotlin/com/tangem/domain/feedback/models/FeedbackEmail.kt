package com.tangem.domain.feedback.models

import java.io.File

data class FeedbackEmail(
    val address: String,
    val subject: String,
    val message: String,
    val file: File? = null,
)