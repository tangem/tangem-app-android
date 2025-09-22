package com.tangem.common.ui.account

import androidx.compose.runtime.Immutable
import arrow.core.Either
import arrow.core.raise.either
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.AccountName

/**
 * Represents a user model (UM) for an [AccountName] in the UI layer.
 * This sealed interface provides a way to handle different types of account names.
 */
@Immutable
sealed interface AccountNameUM {

    /** The textual representation of the account name */
    val value: TextReference

    /**
     * Represents the default main account name.
     * If the user renames the main account, it will be converted to a [Custom] account name.
     */
    data object DefaultMain : AccountNameUM {

        override val value: TextReference = resourceReference(R.string.account_main_account_title)
    }

    /**
     * Represents a custom account name provided by the user
     *
     * @property raw the raw string value of the custom account name
     */
    class Custom(internal val raw: String) : AccountNameUM {

        override val value: TextReference = stringReference(value = raw)
    }
}

/** Extension function to convert a domain model [AccountName] to its corresponding UI model [AccountNameUM] */
fun AccountName.toUM(): AccountNameUM {
    return when (this) {
        is AccountName.Custom -> AccountNameUM.Custom(raw = value)
        AccountName.DefaultMain -> AccountNameUM.DefaultMain
    }
}

/** Extension function to convert a UI model [AccountNameUM] to its corresponding domain model [AccountName] */
fun AccountNameUM.toDomain(): Either<AccountName.Error, AccountName> = either {
    when (this@toDomain) {
        is AccountNameUM.Custom -> AccountName(value = raw).bind()
        AccountNameUM.DefaultMain -> AccountName.DefaultMain
    }
}