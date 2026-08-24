package com.tangem.domain.tokens

/**
 * Tokens feature toggles
 *
[REDACTED_AUTHOR]
 */
interface TokensFeatureToggles {

    /**
     * Gates the migration of `CryptoCurrencyStatus` staking/yield balances to the generic
     * `BalanceContribution` summation path.
     *
     * **Live, not dormant:** with this on, `DefaultSingleAccountStatusListProducer` collects the registered
     * `BalanceContributionProvider`s and `CryptoCurrencyStatusFactory` fills
     * `CryptoCurrencyStatus.Value.contributions` *instead of* the legacy `stakingBalance` field, which it leaves
     * `null`. Only readers that go through `getExtraBalanceOrNull()` / `stakingBalanceData` follow the move.
     *
     * Phase 2 still has to migrate the direct `stakingBalance` readers — `TotalFiatBalanceCalculator` and
     * `DefaultCurrencyChecksRepository` among them — so until then this must stay `"undefined"` in
     * `feature_toggles_config.json` (dev/tester builds only).
     */
    val isBalanceContributionsEnabled: Boolean
}