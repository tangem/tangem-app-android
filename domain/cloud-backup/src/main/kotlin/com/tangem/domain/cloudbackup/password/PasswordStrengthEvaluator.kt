package com.tangem.domain.cloudbackup.password

import java.nio.CharBuffer
import java.text.Normalizer

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

    /**
     * Same as [evaluate], but `null` while [password] is still too short to rate — the strength meter is
     * only shown from [SHORT_MAX_LENGTH] + 1 characters on (FR-09).
     */
    fun evaluateRated(password: CharArray): PasswordStrength? = evaluateRated(CharBuffer.wrap(password))

    fun evaluateRated(password: CharSequence): PasswordStrength? {
        val normalized = password.nfc()
        return if (normalized.length <= SHORT_MAX_LENGTH) null else evaluate(normalized)
    }

    fun evaluate(password: CharSequence): PasswordStrength {
        val normalized = password.nfc()
        val classes = classesOf(normalized)
        return when {
            normalized.length >= MIN_LENGTH && classes.count == ALL_CLASSES -> PasswordStrength.STRONG
            normalized.length >= MEDIUM_MIN_LENGTH && classes.count >= MEDIUM_MIN_CLASSES -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }

    fun hint(password: CharArray): PasswordStrengthHint = hint(CharBuffer.wrap(password))

    fun hint(rawPassword: CharSequence): PasswordStrengthHint {
        val password = rawPassword.nfc()
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

    /**
     * The cipher NFC-normalizes the password before deriving the key (CPR-06), so the strength must be rated on
     * the same form: a decomposed `e` + U+0301 counts as two characters — one of them "special" — while the key
     * is derived from the single precomposed `é`. Already-normalized input (every ASCII password) is returned as
     * is, so the zero-copy path is kept; only denormalized input materializes a short-lived String.
     */
    private fun CharSequence.nfc(): CharSequence {
        val form = Normalizer.Form.NFC
        return if (Normalizer.isNormalized(this, form)) this else Normalizer.normalize(this, form)
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