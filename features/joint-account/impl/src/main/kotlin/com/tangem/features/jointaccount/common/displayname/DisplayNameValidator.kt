package com.tangem.features.jointaccount.common.displayname

/**
 * Validates a member's display name: 1–[MAX_LENGTH] characters, any of them — letters, digits, spaces, emoji and
 * special characters are all allowed, same as for the account name.
 *
 * The value is never normalised — no trimming, no case folding, no Unicode normalisation — because the name
 * becomes part of the payload signed by the card. Validation is a boolean gate only.
 */
internal class DisplayNameValidator {

    /** Any characters are allowed, but a name made only of blanks is still not a name */
    fun isValid(name: String): Boolean {
        return name.isNotBlank() && name.length <= MAX_LENGTH
    }

    companion object {
        const val MAX_LENGTH = 25
    }
}