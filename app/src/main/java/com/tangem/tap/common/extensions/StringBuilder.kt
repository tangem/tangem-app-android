package com.tangem.tap.common.extensions

fun StringBuilder.breakLine(count: Int = 1): StringBuilder = append("\n".repeat(count))