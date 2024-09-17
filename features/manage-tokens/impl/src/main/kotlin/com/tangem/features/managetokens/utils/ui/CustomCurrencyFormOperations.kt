package com.tangem.features.managetokens.utils.ui

import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.managetokens.ValidateTokenFormUseCase
import com.tangem.domain.managetokens.model.exceptoin.CustomTokenFormValidationException
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM
import com.tangem.features.managetokens.entity.customtoken.TextInputFieldUM
import com.tangem.features.managetokens.impl.R
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

internal fun CustomTokenFormUM.updateTokenForm(
    block: CustomTokenFormUM.TokenFormUM.() -> CustomTokenFormUM.TokenFormUM,
): CustomTokenFormUM {
    val form = tokenForm ?: return this

    val updatedForm = form.block()

    return copy(tokenForm = updatedForm)
}

internal fun TextInputFieldUM.updateValue(
    value: String = this.value,
    error: TextReference? = this.error,
    isEnabled: Boolean = this.isEnabled,
    clearError: Boolean = false,
): TextInputFieldUM {
    return copy(
        value = value,
        error = if (clearError) null else error,
        isEnabled = isEnabled,
    )
}

internal fun CustomTokenFormUM.updateWithProgress(
    showProgress: Boolean,
    isWasFilled: Boolean = this.tokenForm?.wasFilled ?: false,
    canAddToken: Boolean = this.canAddToken,
    clearNotifications: Boolean = false,
    clearFieldErrors: Boolean = false,
    disableSecondaryFields: Boolean = false,
): CustomTokenFormUM {
    return copy(
        isValidating = showProgress,
        canAddToken = canAddToken,
        notifications = if (clearNotifications) persistentListOf() else notifications,
    ).updateTokenForm {
        copy(
            contractAddress = contractAddress.updateValue(
                clearError = clearFieldErrors,
            ),
            name = name.updateValue(
                isEnabled = !showProgress && !disableSecondaryFields,
                clearError = clearFieldErrors,
            ),
            symbol = symbol.updateValue(
                isEnabled = !showProgress && !disableSecondaryFields,
                clearError = clearFieldErrors,
            ),
            decimals = decimals.updateValue(
                isEnabled = !showProgress && !disableSecondaryFields,
                clearError = clearFieldErrors,
            ),
            wasFilled = isWasFilled,
        )
    }
}

internal fun CustomTokenFormUM.updateWithCurrency(currency: CryptoCurrency): CustomTokenFormUM {
    return updateTokenForm {
        copy(
            contractAddress = contractAddress.updateValue(error = null),
            name = name.updateValue(currency.name),
            symbol = symbol.updateValue(currency.symbol),
            decimals = decimals.updateValue(currency.decimals.toString()),
        )
    }
}

internal fun CustomTokenFormUM.updateWithContractAddressException(
    exception: CustomTokenFormValidationException.ContractAddress,
): CustomTokenFormUM {
    return updateTokenForm {
        copy(
            contractAddress = contractAddress.updateValue(
                error = when (exception) {
                    CustomTokenFormValidationException.ContractAddress.Empty -> {
                        null
                    }
                    CustomTokenFormValidationException.ContractAddress.Invalid -> {
                        resourceReference(R.string.custom_token_creation_error_invalid_contract_address)
                    }
                },
            ),
        )
    }
}

internal fun CustomTokenFormUM.updateWithDecimalsException(
    exception: CustomTokenFormValidationException.Decimals,
): CustomTokenFormUM {
    return updateTokenForm {
        copy(
            decimals = decimals.updateValue(
                error = when (exception) {
                    is CustomTokenFormValidationException.Decimals.Empty -> {
                        null
                    }
                    is CustomTokenFormValidationException.Decimals.Invalid -> {
                        resourceReference(
                            R.string.custom_token_creation_error_wrong_decimals,
                            wrappedList(ValidateTokenFormUseCase.MAX_DECIMALS),
                        )
                    }
                },
            ),
        )
    }
}

internal fun CustomTokenFormUM.updateWithCurrencyNotFoundNotification(): CustomTokenFormUM {
    val notification = CustomTokenFormUM.NotificationUM(
        id = "currency_not_found",
        config = NotificationConfig(
            title = resourceReference(R.string.custom_token_validation_error_not_found_title),
            subtitle = resourceReference(R.string.custom_token_validation_error_not_found_description),
            iconResId = R.drawable.img_attention_20,
        ),
    )

    return copy(
        notifications = notifications.mutate {
            it.add(notification)
        },
    )
}

internal fun CustomTokenFormUM.updateWithCurrencyAlreadyAddedNotification(): CustomTokenFormUM {
    val notification = CustomTokenFormUM.NotificationUM(
        id = "currency_already_added",
        config = NotificationConfig(
            title = resourceReference(R.string.custom_token_creation_error_token_already_exist_title),
            subtitle = resourceReference(R.string.custom_token_creation_error_token_already_exist_message),
            iconResId = R.drawable.img_attention_20,
        ),
    )

    return copy(
        notifications = notifications.mutate {
            it.add(notification)
        },
    )
}