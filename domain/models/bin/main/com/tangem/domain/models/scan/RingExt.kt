package com.tangem.domain.models.scan

import com.tangem.domain.models.scan.CardDTO.Companion.RING_BATCH_IDS
import com.tangem.domain.models.scan.CardDTO.Companion.RING_BATCH_PREFIX

fun isRing(batchId: String): Boolean {
    return RING_BATCH_IDS.contains(batchId) || batchId.startsWith(RING_BATCH_PREFIX)
}