package com.tangem.features.managetokens.component.preview

import androidx.compose.foundation.lazy.LazyListScope
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.managetokens.component.CustomTokenFormComponent
import com.tangem.features.managetokens.entity.ClickableFieldUM
import com.tangem.features.managetokens.entity.CustomTokenFormUM
import com.tangem.features.managetokens.entity.TextInputFieldUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.customTokenFormContent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class PreviewCustomTokenFormComponent(
    networkName: ClickableFieldUM = ClickableFieldUM(
        label = resourceReference(R.string.custom_token_network_input_title),
        value = stringReference(value = "Ethereum"),
        onClick = {},
    ),
    derivationPath: ClickableFieldUM = ClickableFieldUM(
        label = resourceReference(R.string.custom_token_derivation_path),
        value = stringReference(value = "Default"),
        onClick = {},
    ),
    canAddToken: Boolean = false,
    contractAddress: TextInputFieldUM = TextInputFieldUM(
        label = resourceReference(R.string.custom_token_contract_address_input_title),
        placeholder = stringReference(value = "0x000000000000000000000000000"),
        value = "",
        onValueChange = {},
    ),
    tokenName: TextInputFieldUM = TextInputFieldUM(
        label = resourceReference(R.string.custom_token_name_input_title),
        placeholder = stringReference(value = "E.g. USD Coin"),
        value = "",
        onValueChange = {},
    ),
    tokenSymbol: TextInputFieldUM = TextInputFieldUM(
        label = resourceReference(R.string.custom_token_token_symbol_input_title),
        placeholder = stringReference(value = "E.g. USDC"),
        value = "",
        onValueChange = {},
    ),
    tokenDecimals: TextInputFieldUM = TextInputFieldUM(
        label = resourceReference(R.string.custom_token_decimals_input_title),
        placeholder = stringReference(value = "8"),
        value = "",
        onValueChange = {},
    ),
    notifications: ImmutableList<CustomTokenFormUM.NotificationUM> = persistentListOf(
        CustomTokenFormUM.NotificationUM(
            id = "1",
            config = NotificationConfig(
                title = stringReference(value = "Note that tokens can be created by anyone"),
                subtitle = stringReference(value = "Be aware of adding scam tokens, they can cost nothing"),
                iconResId = R.drawable.img_attention_20,
            ),
        ),
    ),
) : CustomTokenFormComponent {

    private val previewState = CustomTokenFormUM(
        networkName = networkName,
        contractAddress = contractAddress,
        tokenName = tokenName,
        tokenSymbol = tokenSymbol,
        tokenDecimals = tokenDecimals,
        derivationPath = derivationPath,
        notifications = notifications,
        canAddToken = canAddToken,
        onDerivationPathClick = {},
        onNetworkClick = {},
        onAddClick = {},
    )

    override fun content(scope: LazyListScope) {
        scope.customTokenFormContent(model = previewState)
    }
}