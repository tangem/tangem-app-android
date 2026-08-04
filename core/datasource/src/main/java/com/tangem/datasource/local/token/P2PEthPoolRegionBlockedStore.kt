package com.tangem.datasource.local.token

import com.tangem.core.local.datastore.RuntimeStateStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory flag: the last P2P vaults request returned HTTP 451 (region unavailable).
 * `true` → staking is blocked in the region. Reset to `false` on a successful fetch.
 */
@Singleton
class P2PEthPoolRegionBlockedStore @Inject constructor() :
    RuntimeStateStore<Boolean> by RuntimeStateStore(defaultValue = false)