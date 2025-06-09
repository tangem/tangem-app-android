package com.tangem.features.managetokens.utils.ui

import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.managetokens.ValidateTokenFormUseCase
import com.tangem.domain.managetokens.model.exceptoin.CustomTokenFormValidationException
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM.TokenFormUM.Field
import com.tangem.features.managetokens.impl.R
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentMap

internal fun CustomTokenFormUM.updateTokenForm(
    block: CustomTokenFormUM.TokenFormUM.() -> CustomTokenFormUM.TokenFormUM,
): CustomTokenFormUM {
    val form = tokenForm ?: return this

    val updatedForm = form.block()

    return copy(tokenForm = updatedForm)
}

internal fun CustomTokenFormUM.updateWithProgress(
    showProgress: Boolean,
    isWasFilled: Boolean = this.tokenForm?.wasFilled ?: false,
    canAddToken: Boolean = this.canAddToken,
    needToAddDerivation: Boolean = false,
    clearNotifications: Boolean = false,
    clearFieldErrors: Boolean = false,
    disableSecondaryFields: Boolean = false,
): CustomTokenFormUM {
    return copy(
        isValidating = showProgress,
        canAddToken = canAddToken,
        needToAddDerivation = needToAddDerivation,
        notifications = if (clearNotifications) persistentListOf() else notifications,
    ).updateTokenForm {
        val updatedFields = fields.mapValues { (key, field) ->
            field.copy(
                isEnabled = when (key) {
                    Field.CONTRACT_ADDRESS -> field.isEnabled
                    Field.NAME,
                    Field.SYMBOL,
                    Field.DECIMALS,
                    -> !(showProgress || disableSecondaryFields)
                },
                error = if (clearFieldErrors) null else field.error,
            )
        }

        copy(
            fields = updatedFields.toPersistentMap(),
            wasFilled = isWasFilled,
        )
    }
}

internal fun CustomTokenFormUM.updateWithCurrency(currency: CryptoCurrency): CustomTokenFormUM {
    return updateTokenForm {
        val updatedFields = fields.mapValues { (key, field) ->
            when (key) {
                Field.CONTRACT_ADDRESS -> field.copy(
                    error = null,
                )
                Field.NAME -> field.copy(
                    value = currency.name,
                    error = null,
                    isEnabled = false,
                )
                Field.SYMBOL -> field.copy(
                    value = currency.symbol,
                    error = null,
                    isEnabled = false,
                )
                Field.DECIMALS -> field.copy(
                    value = currency.decimals.toString(),
                    error = null,
                    isEnabled = false,
                )
            }
        }

        copy(
            fields = updatedFields.toPersistentMap(),
        )
    }
}

internal fun CustomTokenFormUM.updateWithContractAddressException(
    exception: CustomTokenFormValidationException.ContractAddress,
): CustomTokenFormUM {
    return updateTokenForm {
        val updatedFields = fields.mutate {
            it[Field.CONTRACT_ADDRESS] = it.getValue(Field.CONTRACT_ADDRESS).copy(
                error = when (exception) {
                    CustomTokenFormValidationException.ContractAddress.Invalid -> {
                        resourceReference(R.string.custom_token_creation_error_invalid_contract_address)
                    }
                },
            )
        }

        copy(fields = updatedFields)
    }
}

internal fun CustomTokenFormUM.updateWithDecimalsException(
    exception: CustomTokenFormValidationException.Decimals,
): CustomTokenFormUM {
    return updateTokenForm {
        val updatedFields = fields.mutate {
            it[Field.DECIMALS] = it.getValue(Field.DECIMALS).copy(
                error = when (exception) {
                    is CustomTokenFormValidationException.Decimals.Empty -> {
                        null // Should not display this error
                    }
                    is CustomTokenFormValidationException.Decimals.Invalid -> {
                        resourceReference(
                            R.string.custom_token_creation_error_wrong_decimals,
                            wrappedList(ValidateTokenFormUseCase.MAX_DECIMALS),
                        )
                    }
                },
            )
        }

        copy(fields = updatedFields)
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