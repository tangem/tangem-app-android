package com.tangem.domain.card

import com.tangem.domain.card.models.ScanConfig

interface ScanCardProcessor {

    suspend fun scan(config: ScanConfig)
}