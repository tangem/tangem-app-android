package com.domain.blockaid.models.transaction

/**
 * Result of BlockAid's transaction validation
 */
enum class ValidationResult {

    /**
     * Transaction was confirmed safe
     */
    SAFE,

    /**
     * Transaction was confirmed unsafe
     */
    UNSAFE,

    /**
     * Validation wasn't performed, BlockAid cannot guarantee transaction's safety
     */
    FAILED_TO_VALIDATE,
}