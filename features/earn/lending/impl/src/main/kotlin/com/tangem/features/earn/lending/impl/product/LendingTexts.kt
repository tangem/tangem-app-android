package com.tangem.features.earn.lending.impl.product

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import java.math.BigInteger

internal object LendingTexts {

    val title: TextReference = resourceReference(R.string.earn_product_lending_on_aave)

    val pendingTransaction: TextReference = resourceReference(R.string.lending_error_previous_transaction_processing)

    val updateRequired: TextReference = resourceReference(R.string.lending_update_app_message)

    // Temporary plain text, no Lokalise key: neither the PRD nor the iOS app has this state.
    val depositsPaused: TextReference = stringReference("Deposits are paused by the provider.")

    fun gasLimitTooLow(required: BigInteger): TextReference =
        resourceReference(R.string.lending_error_gas_limit_too_low, wrappedList(required.toString()))
}