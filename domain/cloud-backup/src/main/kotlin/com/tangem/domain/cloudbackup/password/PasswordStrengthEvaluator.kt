package com.tangem.domain.cloudbackup.password

import java.nio.CharBuffer

/**
 * Evaluates the [PasswordStrength] of a backup password.
 *
 * Criteria: length >= [MIN_LENGTH], an uppercase letter, a lowercase letter, a digit, a special
 * (non-alphanumeric) character.
 * - [PasswordStrength.STRONG] — all five criteria are met.
 * - [PasswordStrength.MEDIUM] — length >= [MEDIUM_MIN_LENGTH] and at least two of the four character-class criteria.
 * - [PasswordStrength.WEAK] — otherwise.
 *
 * Neither overload copies the sensitive password: the [CharArray] one wraps it in a zero-copy
 * [CharBuffer] view, and the [CharSequence] one reads the [String] (or other sequence) in place.
 */
object PasswordStrengthEvaluator {

    const val MIN_LENGTH = 8

    private const val MEDIUM_MIN_LENGTH = 6
    private const val SHORT_MAX_LENGTH = 3
    private const val ALL_CLASSES = 4
    private const val MEDIUM_MIN_CLASSES = 2

    fun evaluate(password: CharArray): PasswordStrength = evaluate(CharBuffer.wrap(password))

    fun evaluate(password: CharSequence): PasswordStrength {
        val classes = classesOf(password)
        return when {
            password.length >= MIN_LENGTH && classes.count == ALL_CLASSES -> PasswordStrength.STRONG
            password.length >= MEDIUM_MIN_LENGTH && classes.count >= MEDIUM_MIN_CLASSES -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }

    fun hint(password: CharArray): PasswordStrengthHint = hint(CharBuffer.wrap(password))

    fun hint(password: CharSequence): PasswordStrengthHint {
        val length = password.length
        if (length <= SHORT_MAX_LENGTH) return PasswordStrengthHint.USE_ALL_CRITERIA
        if (length <= MEDIUM_MIN_LENGTH) return PasswordStrengthHint.KEEP_GOING

        val classes = classesOf(password)
        return when {
            !classes.hasSpecial -> PasswordStrengthHint.ADD_SYMBOL
            !classes.hasDigit -> PasswordStrengthHint.ADD_NUMBER
            !classes.hasUppercase -> PasswordStrengthHint.ADD_UPPERCASE
            !classes.hasLowercase -> PasswordStrengthHint.ADD_LOWERCASE
            length < MIN_LENGTH -> PasswordStrengthHint.ALMOST_LONG
            else -> PasswordStrengthHint.STRONG
        }
    }

    private fun classesOf(password: CharSequence): CharClasses {
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
        return CharClasses(
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasDigit = hasDigit,
            hasSpecial = hasSpecial,
        )
    }

    private data class CharClasses(
        val hasUppercase: Boolean,
        val hasLowercase: Boolean,
        val hasDigit: Boolean,
        val hasSpecial: Boolean,
    ) {
        val count: Int get() = listOf(hasUppercase, hasLowercase, hasDigit, hasSpecial).count { it }
    }
}