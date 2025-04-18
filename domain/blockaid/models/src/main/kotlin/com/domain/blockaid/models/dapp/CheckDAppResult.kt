package com.domain.blockaid.models.dapp

/**
 * Result of BlockAid's DApp domain check
 */
enum class CheckDAppResult {

    /**
     * DApp was confirmed safe
     */
    SAFE,

    /**
     * DApp was confirmed unsafe (known security risk)
     */
    UNSAFE,

    /**
     * Check wasn't performed, BlockAid cannot guarantee DApp's safety
     */
    FAILED_TO_VERIFY,
}