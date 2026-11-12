package com.tangem.domain.models.yield.supply

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.contributionOrNull

/**
 * Yield supply status of this currency, or `null` when yield supply is not active on it.
 *
 * Read yield through this accessor, never through [CryptoCurrencyStatus.Value.yieldSupplyStatus]. The field comes from
 * the SDK balance read, which reports a token as yield supply only while it is active; the [YieldSupplyContribution]
 * answers when the field is empty. An inactive contribution is answered as `null` too, so both sources keep the
 * field's meaning: readers rely on it — a non-null status routes a send through the Yield Module.
 * The field and the `yieldSupplyStatus ?:` half go away with TWI_1948_YIELD_CONTRIBUTION_ENABLED.
 */
val CryptoCurrencyStatus.Value.yieldSupply: YieldSupplyStatus?
    get() = yieldSupplyStatus ?: contributionOrNull<YieldSupplyContribution>()?.status?.takeIf { it.isActive }

/**
 * Freshness of the Yield Mode state. It rides on [YieldSupplyContribution] rather than on a dedicated
 * [CryptoCurrencyStatus.Sources] field, as [CryptoCurrencyStatus.Sources.contributionSources] replaces those.
 * `null` when the currency has no Yield Mode state.
 */
val CryptoCurrencyStatus.Value.yieldSupplySource: StatusSource?
    get() = contributionOrNull<YieldSupplyContribution>()?.source

/**
 * [yieldSupplySource] of an active yield supply, whose supplied part is folded into `amount`, so the amount is only as
 * fresh as this source. `null` when yield supply is not active or carries no source.
 */
val CryptoCurrencyStatus.Value.activeYieldSupplySource: StatusSource?
    get() = yieldSupplySource?.takeIf { yieldSupply?.isActive == true }

/**
 * Whether `amount` is up to date: the network status it comes from and, when one is folded into it, the active yield
 * supply ([activeYieldSupplySource]) are both ACTUAL.
 */
val CryptoCurrencyStatus.Value.isSpendableAmountActual: Boolean
    get() = sources.networkSource.isActual() && activeYieldSupplySource?.isActual() != false