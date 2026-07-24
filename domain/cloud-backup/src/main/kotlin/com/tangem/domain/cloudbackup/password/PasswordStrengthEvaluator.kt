package com.tangem.domain.cloudbackup.password

import java.nio.CharBuffer

/**
 * Evaluates the [PasswordStrength] of a backup password.
 *
 * Criteria: length >= [MIN_LENGTH], an uppercase letter, a lowercase letter, a digit, a special
 * (non-alphanumeric) character.
 * - [PasswordStrength.STRONG] — all five criteria are met.
 * - [PasswordStrength.MEDIUM] — length is met and at least two of the four character-class criteria.
 * - [PasswordStrength.WEAK] — otherwise.
 *
 * Neither overload copies the sensitive password: the [CharArray] one wraps it in a zero-copy
 * [CharBuffer] view, and the [CharSequence] one reads the [String] (or other sequence) in place.
 */
object PasswordStrengthEvaluator {

    const val MIN_LENGTH = 8

    private const val ALL_CLASSES = 4
    private const val MEDIUM_MIN_CLASSES = 2

    fun evaluate(password: CharArray): PasswordStrength = evaluate(CharBuffer.wrap(password))

    fun evaluate(password: CharSequence): PasswordStrength {
        val hasMinLength = password.length >= MIN_LENGTH

        var hasUppercase = false
        var hasLowercase = false
        var hasDigit = false
        var hasSpecial = false
        for (char in password) {
            when {
                char.isUpperCase() -> hasUppercase = true
                char.isLowerCase() -> hasLowercase = true
                char.isDigit() -> hasDigit = true
                !char.isLetterOrDigit() -> hasSpecial = true
            }
        }

        val classCount = listOf(hasUppercase, hasLowercase, hasDigit, hasSpecial).count { it }

        return when {
            hasMinLength && classCount == ALL_CLASSES -> PasswordStrength.STRONG
            hasMinLength && classCount >= MEDIUM_MIN_CLASSES -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }
}