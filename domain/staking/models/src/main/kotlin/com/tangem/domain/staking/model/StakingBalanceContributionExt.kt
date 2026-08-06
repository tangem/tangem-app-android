package com.tangem.domain.staking.model

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.contributionOrNull
import com.tangem.domain.models.staking.StakingBalance

/**
 * Staking balance of this currency, or `null` when it has none.
 *
 * Reads [CryptoCurrencyStatus.Value.contributions] first and falls back to the legacy typed field — the same gate
 * `common/.../getExtraBalanceOrNull()` uses for totals. No call site reads `TWI_1717_BALANCE_CONTRIBUTIONS`,
 * because an empty contribution list *is* what "toggle off" looks like. Every reader of the staking breakdown
 * ("Axis 2") goes through here, so [CryptoCurrencyStatus.Value.stakingBalance] can be deleted without touching
 * them again.
 *
 * Lives in this module, not in `domain/models`, on purpose: the base module must not name a concrete balance type,
 * or moving the staking cluster out of it stays a Gradle cycle. The generic half is
 * [contributionOrNull], which selects by type without knowing any of them.
 *
 * Deliberately **not** named `stakingBalance`: a member property silently wins over a same-named extension, so
 * that name would compile and go on reading the legacy field forever.
 *
 * [StakingBalance.Data] is the only variant a status ever carries — `CryptoCurrencyStatusFactory` maps
 * [StakingBalance.Empty] and [StakingBalance.Error] to `null` — so narrowing to it loses nothing. **Both** halves
 * must narrow: `Empty` and `Error` are `BalanceContribution`s in their own right, so an un-narrowed lookup on
 * either side could hand a caller a variant with no breakdown to show.
 */
val CryptoCurrencyStatus.Value.stakingBalanceData: StakingBalance.Data?
    get() = contributionOrNull<StakingBalance.Data>() ?: stakingBalance as? StakingBalance.Data

/**
 * How fresh this currency's staking data is — `ACTUAL` when it has no staking balance at all, which is what
 * `CryptoCurrencyStatusFactory` stamps for that case anyway.
 *
 * The source half of [stakingBalanceData].
 *
 * Callers that also depend on the on-chain amount must still `&&` this with
 * [CryptoCurrencyStatus.Sources.networkSource] — this answers only for the staking half.
 */
val CryptoCurrencyStatus.Value.stakingSource: StatusSource
    get() = stakingBalanceData?.source ?: sources.stakingBalanceSource