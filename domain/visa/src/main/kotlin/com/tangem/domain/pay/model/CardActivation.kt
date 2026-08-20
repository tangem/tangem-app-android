package com.tangem.domain.pay.model

const val CARD_ACTIVATION_LAST_DIGITS_LENGTH = 4

fun String.isValidCardLastDigits(): Boolean = length == CARD_ACTIVATION_LAST_DIGITS_LENGTH && all(Char::isDigit)