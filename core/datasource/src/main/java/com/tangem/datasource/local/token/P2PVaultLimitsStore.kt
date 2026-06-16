package com.tangem.datasource.local.token

import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.domain.staking.model.ethpool.VaultLimitInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory store for P2P vault limits from Tangem API /v1/coins/settings.
 * Map key is vaultAddress.lowercase(). Null map value means limits not yet fetched.
 * A missing key means the vault is full (null-limit vaults are excluded at fetch time).
 */
@Singleton
class P2PVaultLimitsStore @Inject constructor() :
    RuntimeStateStore<Map<String, VaultLimitInfo>?> by RuntimeStateStore(defaultValue = null)