package com.tangem.tap.domain.moduleMessage.domain.converter

import android.content.Context
import com.tangem.common.module.ModuleMessageConverter
import com.tangem.domain.AddCustomTokenError
import com.tangem.domain.DomainModuleError
import com.tangem.tap.domain.moduleMessage.ConvertedMessage
import com.tangem.tap.domain.moduleMessage.ConvertedStringMessage
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 09.10.2022.
 */
internal class AddCustomTokenErrorConverter(
    private val context: Context,
) : ModuleMessageConverter<DomainModuleError, ConvertedMessage?> {

    @Suppress("MagicNumber")
    override fun convert(message: DomainModuleError): ConvertedMessage? {
        val customTokenError = message as? AddCustomTokenError ?: throw UnsupportedOperationException()

        val rawMessage = when (customTokenError) {
            AddCustomTokenError.Warning.PotentialScamToken -> R.string.custom_token_validation_error_not_found
            AddCustomTokenError.Warning.TokenAlreadyAdded -> R.string.custom_token_validation_error_already_added
            AddCustomTokenError.Warning.UnsupportedSolanaToken -> R.string.alert_manage_tokens_unsupported_message
            AddCustomTokenError.InvalidContractAddress -> R.string.custom_token_creation_error_invalid_contract_address
            AddCustomTokenError.NetworkIsNotSelected -> R.string.custom_token_creation_error_network_not_selected
            AddCustomTokenError.InvalidDerivationPath -> R.string.custom_token_creation_error_invalid_derivation_path
            AddCustomTokenError.InvalidDecimalsCount -> {
                context.getString(R.string.custom_token_creation_error_wrong_decimals, 30)
            }
            AddCustomTokenError.FieldIsEmpty -> R.string.custom_token_creation_error_required_field
            else -> null
        }

        return when (rawMessage) {
            is Int -> ConvertedStringMessage(context.getString(rawMessage))
            is String -> ConvertedStringMessage(rawMessage)
            else -> null
        }
    }
}
