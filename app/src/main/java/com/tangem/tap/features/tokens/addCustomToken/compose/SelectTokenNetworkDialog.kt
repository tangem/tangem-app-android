package com.tangem.tap.features.tokens.addCustomToken.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tangem.domain.DomainDialog
import com.tangem.tap.common.compose.SimpleDialog
import com.tangem.tap.common.compose.TitleSubtitle
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
@Composable
fun SelectTokenNetworkDialog(dialog: DomainDialog.SelectTokenDialog, onDismissRequest: () -> Unit) {
    SimpleDialog(
        title = stringResource(id = R.string.custom_token_type_network),
        items = dialog.items,
        onSelect = dialog.onSelect,
        onDismissRequest = onDismissRequest
    ) { network -> TitleSubtitle(dialog.networkIdConverter(network.networkId), network.address ?: "") }
}