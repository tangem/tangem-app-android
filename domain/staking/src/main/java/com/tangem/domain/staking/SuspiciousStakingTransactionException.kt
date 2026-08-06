package com.tangem.domain.staking

/**
 * Non-fatal marker reported to Firebase when pre-sign staking transaction verification flags a
 * transaction as non-[StakingTransactionVerdict.SAFE] (i.e. [StakingTransactionVerdict.WARNING] or
 * [StakingTransactionVerdict.UNSAFE]). Used purely for observability — the actual block/warn
 * decision is handled by the verdict itself.
 *
 * The message is intentionally stable (verdict lives in the event params) so Crashlytics groups all
 * occurrences under a single issue.
 */
class SuspiciousStakingTransactionException(
    verdict: StakingTransactionVerdict,
) : Exception("Staking transaction verification flagged a transaction as $verdict")