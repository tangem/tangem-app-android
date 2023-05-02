package com.tangem.domain.card.model

import com.tangem.domain.card.ScanCardException
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.models.scan.ScanResponse

/**
 * Represents the parameters to be used during a card scanning operation.
 *
 * @property cardId an optional card ID to scan. If null, can scan any card present.
 * Defaults to null.
 * @property allowRequestAccessCodeFromRepository whether to prompt the user for an access code if needed.
 * Defaults to false.
 * @property afterScanChains An array of chains that should be executed after a successful card scan operation.
 * Defaults to an empty array.
 */
class ScanCardParams(
    val cardId: String? = null,
    val allowRequestAccessCodeFromRepository: Boolean = false,
    val afterScanChains: Array<out Chain<ScanCardException.ChainException, ScanResponse>> = emptyArray(),
)
