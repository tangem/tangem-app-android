package com.tangem.datasource.local.config.issuers

import com.tangem.datasource.local.config.issuers.models.Issuer

/**
 * Storage for list of Twins [Issuer]
 *
[REDACTED_AUTHOR]
 */
interface IssuersConfigStorage {

    /** Get config */
    suspend fun getConfig(): List<Issuer>
}