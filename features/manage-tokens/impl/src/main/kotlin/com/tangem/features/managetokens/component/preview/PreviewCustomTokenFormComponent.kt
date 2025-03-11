package com.tangem.features.managetokens.component.preview

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.managetokens.component.CustomTokenFormComponent
import com.tangem.features.managetokens.entity.customtoken.ClickableFieldUM
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM
import com.tangem.features.managetokens.entity.customtoken.TextInputFieldUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.CustomTokenFormContent
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentMap

internal class PreviewCustomTokenFormComponent(
    networkName: ClickableFieldUM = PreviewCustomTokenFormComponent.networkName,
    derivationPath: ClickableFieldUM? = PreviewCustomTokenFormComponent.derivationPath,
    canAddToken: Boolean = false,
    tokenForm: CustomTokenFormUM.TokenFormUM? = PreviewCustomTokenFormComponent.tokenForm,
    notifications: PersistentList<CustomTokenFormUM.NotificationUM> = PreviewCustomTokenFormComponent.notifications,
) : CustomTokenFormComponent {

    private val previewState = CustomTokenFormUM(
        networkName = networkName,
        tokenForm = tokenForm,
        derivationPath = derivationPath,
        notifications = notifications,
        canAddToken = canAddToken,
        saveToken = {},
    )

    @Composable
    override fun Content(modifier: Modifier) {
        CustomTokenFormContent(modifier = modifier, model = previewState)
    }

    companion object {
        val networkName: ClickableFieldUM = ClickableFieldUM(
            label = resourceReference(R.string.custom_token_network_input_title),
            value = stringReference(value = "Ethereum"),
            onClick = {},
        )
        val derivationPath: ClickableFieldUM = ClickableFieldUM(
            label = resourceReference(R.string.custom_token_derivation_path),
            value = stringReference(value = "Default"),
            onClick = {},
        )
        val tokenForm: CustomTokenFormUM.TokenFormUM = CustomTokenFormUM.TokenFormUM(
            fields = mapOf(
                CustomTokenFormUM.TokenFormUM.Field.CONTRACT_ADDRESS to TextInputFieldUM(
                    label = resourceReference(R.string.custom_token_contract_address_input_title),
                    placeholder = stringReference(value = "0x000000000000000000000000000"),
                    value = "",
                    keyboardOptions = KeyboardOptions(),
                    onValueChange = {},
                    onFocusChange = {},
                ),
                CustomTokenFormUM.TokenFormUM.Field.NAME to TextInputFieldUM(
                    label = resourceReference(R.string.custom_token_name_input_title),
                    placeholder = stringReference(value = "E.g. USD Coin"),
                    value = "",
                    keyboardOptions = KeyboardOptions(),
                    onValueChange = {},
                    onFocusChange = {},
                ),
                CustomTokenFormUM.TokenFormUM.Field.SYMBOL to TextInputFieldUM(
                    label = resourceReference(R.string.custom_token_token_symbol_input_title),
                    placeholder = stringReference(value = "E.g. USDC"),
                    value = "",
                    keyboardOptions = KeyboardOptions(),
                    onValueChange = {},
                    onFocusChange = {},
                ),
                CustomTokenFormUM.TokenFormUM.Field.DECIMALS to TextInputFieldUM(
                    label = resourceReference(R.string.custom_token_decimals_input_title),
                    placeholder = stringReference(value = "8"),
                    value = "",
                    keyboardOptions = KeyboardOptions(),
                    onValueChange = {},
                    onFocusChange = {},
                ),
            ).toPersistentMap(),
        )
        val notifications: PersistentList<CustomTokenFormUM.NotificationUM> = persistentListOf(
            CustomTokenFormUM.NotificationUM(
                id = "1",
                config = NotificationConfig(
                    title = stringReference(value = "Note that tokens can be created by anyone"),
                    subtitle = stringReference(value = "Be aware of adding scam tokens, they can cost nothing"),
                    iconResId = R.drawable.img_attention_20,
                ),
            ),
        )
    }
}