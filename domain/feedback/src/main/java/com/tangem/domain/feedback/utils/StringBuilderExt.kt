package com.tangem.domain.feedback.utils

fun StringBuilder.breakLine(count: Int = 1): StringBuilder = append("\n".repeat(count))

fun StringBuilder.skipLine(): StringBuilder = breakLine(count = 2)