package com.tangem.domain.pay.model

/**
 * Cashback block layout variant, from `cashback_display_mode`. Present only when the program is enabled.
 *
 * Absent or unrecognized values fall back to [FULL] (see summary display-mode logic).
 */
enum class CashbackDisplayMode {

    /** Standard cashback block. */
    FULL,

    /** Alternative EU/EEA block. */
    ALT_BLOCK,
    ;

    companion object {
        fun fromString(value: String?): CashbackDisplayMode = when (value?.lowercase()) {
            "alt_block" -> ALT_BLOCK
            else -> FULL
        }
    }
}