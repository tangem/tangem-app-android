package com.tangem.domain.collectibles.account.flow

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Fetcher of the collectibles account balance of a single wallet.
 *
 * Runs on the wallet screen's cadence, so it must never ask the user for anything: no card session, no unlock.
 * A vault it cannot read keeps its cached amount, marked stale — a zero would claim the vault is empty and drop
 * a real balance out of the wallet total.
 */
interface CollectiblesAccountStatusFetcher : FlowFetcher<CollectiblesAccountStatusFetcher.Params> {

    data class Params(val userWalletId: UserWalletId)
}