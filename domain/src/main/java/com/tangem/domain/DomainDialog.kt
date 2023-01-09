package com.tangem.domain

import com.tangem.common.extensions.VoidCallback
import com.tangem.datasource.api.tangemTech.models.Network

/**
 * Created by Anton Zhilenkov on 10/04/2022.
 */
sealed interface DomainDialog {

    data class DialogError(val error: DomainModuleError) : DomainDialog

    data class SelectTokenDialog(
        val items: List<Network>,
        val networkIdConverter: (String) -> String,
        val onSelect: (Network) -> Unit,
        val onClose: VoidCallback = {}
    ) : DomainDialog
}
