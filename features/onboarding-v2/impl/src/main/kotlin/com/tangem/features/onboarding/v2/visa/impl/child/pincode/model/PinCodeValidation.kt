package com.tangem.features.onboarding.v2.visa.impl.child.pincode.model

internal object PinCodeValidation {

    private const val PIN_CODE_LENGTH = 4

    fun validate(pinCode: String): Boolean {
        return sequenceOf(
            ::validateAllDigits,
            ::validateLength,
            ::validateNoRepeatedDigits,
            ::validateNoConsecutiveDigits,
        ).all { it(pinCode) }
    }

    fun validateAllDigits(pinCode: String): Boolean {
        return pinCode.all { it.isDigit() }
    }

    fun validateLength(pinCode: String): Boolean {
        return pinCode.length == PIN_CODE_LENGTH
    }

    private fun validateNoRepeatedDigits(pinCode: String): Boolean {
        return pinCode.toSet().size > 1
    }

    private fun validateNoConsecutiveDigits(pinCode: String): Boolean {
        return pinCode.zipWithNext().any { it.second != it.first + 1 } &&
            pinCode.zipWithNext().any { it.second != it.first - 1 }
    }
}