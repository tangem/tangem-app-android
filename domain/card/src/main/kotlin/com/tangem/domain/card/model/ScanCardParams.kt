package com.tangem.domain.card.model

import com.tangem.domain.card.chains.Chains
import com.tangem.domain.core.utils.TextReference

sealed class ScanCardParams(
    internal val cardId: String? = null,
    internal val message: TextReference? = null,
    internal val allowRequestAccessCodeFromRepository: Boolean = false,
    internal val chains: Array<Chains> = arrayOf(Chains.ScanCard),
) {
    class Simple(
        allowRequestAccessCodeFromRepository: Boolean = false,
    ) : ScanCardParams(allowRequestAccessCodeFromRepository = allowRequestAccessCodeFromRepository)

    class Full(
        allowRequestAccessCodeFromRepository: Boolean = false,
    ) : ScanCardParams(
        allowRequestAccessCodeFromRepository = allowRequestAccessCodeFromRepository,
        chains = arrayOf(Chains.ScanCard, Chains.CheckUnfinishedSaltPayBackup, Chains.Disclaimer),
    )

    class Custom(
        cardId: String? = null,
        message: TextReference? = null,
        allowRequestAccessCodeFromRepository: Boolean = false,
        chains: Array<Chains> = arrayOf(Chains.ScanCard),
    ) : ScanCardParams(cardId, message, allowRequestAccessCodeFromRepository, chains)
}
