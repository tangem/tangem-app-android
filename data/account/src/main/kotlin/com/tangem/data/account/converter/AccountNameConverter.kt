package com.tangem.data.account.converter

import arrow.core.getOrElse
import com.tangem.domain.models.account.AccountName
import com.tangem.utils.converter.TwoWayConverter

/**
 * A converter for transforming [AccountName] domain models into their string representations.
 * This is used to handle the conversion logic between the domain layer and other layers.
 *
[REDACTED_AUTHOR]
 */
internal object AccountNameConverter : TwoWayConverter<AccountName, String?> {

    override fun convert(value: AccountName): String? {
        return when (value) {
            is AccountName.Custom -> value.value
            AccountName.DefaultMain -> null
        }
    }

    override fun convertBack(value: String?): AccountName {
        return if (value == null) {
            AccountName.DefaultMain
        } else {
            AccountName(value = value).getOrElse {
                error("Unable to create AccountName from value: $value. Cause: $it")
            }
        }
    }
}