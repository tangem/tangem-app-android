package com.tangem.domain.staking

/**
 * Result of pre-sign staking transaction verification.
 *
 * - [SAFE]: allowed, no warning (toggle off, pass-through network, Blockaid SAFE, or no Blockaid answer after local confirmation).
 * - [WARNING]: allowed, but Blockaid flagged it as suspicious — surface a warning to the user.
 * - [UNSAFE]: must be blocked (Blockaid UNSAFE, or local recognition failed / missing payload).
 */
enum class StakingTransactionVerdict {
    SAFE,
    WARNING,
    UNSAFE,
}