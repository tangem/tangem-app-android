package com.tangem.domain

import com.tangem.common.extensions.VoidCallback
import com.tangem.network.api.tangemTech.Coins

/**
[REDACTED_AUTHOR]
 */
interface DomainStateDialog

sealed class DomainDialog : DomainStateDialog {

    data class DialogError(val error: DomainError) : DomainDialog()

    data class SelectTokenDialog(
        val items: List<Coins.CheckAddressResponse.Token.Contract>,
        val networkIdConverter: (String) -> String,
        val onSelect: (Coins.CheckAddressResponse.Token.Contract) -> Unit,
        val onClose: VoidCallback = {}
    ) : DomainDialog()
}