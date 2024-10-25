package com.tangem.datasource.local.config.issuers

import com.tangem.datasource.local.config.issuers.models.Issuer

/**
 * Storage for list of Twins [Issuer]
 *
 * @author Andrew Khokhlov on 27/09/2024
 */
interface IssuersConfigStorage {

    /** Get config */
    suspend fun getConfig(): List<Issuer>
}
