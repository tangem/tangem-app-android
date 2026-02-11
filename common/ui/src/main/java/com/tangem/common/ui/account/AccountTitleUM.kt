package com.tangem.common.ui.account

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference

/**
 * A sealed interface representing the title of an account, which can be either a simple text
 * or a more complex account representation with a prefix, name, and icon.
 */
@Immutable
sealed interface AccountTitleUM {

    /** Represents a simple text title. */
    data class Text(
        val title: TextReference,
    ) : AccountTitleUM

    /** Represents an account with a prefix, name, and icon. */
    data class Account(
        val prefixText: TextReference,
        val name: TextReference,
        val icon: AccountIconUM,
    ) : AccountTitleUM {
        companion object {
            fun payment(prefixText: TextReference = TextReference.EMPTY): Account {
                return Account(
                    prefixText = prefixText,
                    name = resourceReference(R.string.tangempay_payment_account),
                    icon = AccountIconUM.Payment,
                )
            }
        }
    }
}