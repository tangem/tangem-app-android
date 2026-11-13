package com.tangem.features.earn.lending.impl.product

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.product.api.product.EarnProvider

// Temporary plain text in this file: replace with localized string resources.
internal object LendingTexts {

    val pendingTransaction: TextReference = stringReference("Wait for the previous transaction to complete.")

    val gasLimitTooLow: TextReference = stringReference("The gas limit is too low for this transaction.")

    val depositsPaused: TextReference = stringReference("Deposits are paused by the provider.")

    fun title(provider: EarnProvider): TextReference = combinedReference(stringReference("Lending on "), provider.title)

    fun updateRequired(provider: EarnProvider): TextReference = combinedReference(
        stringReference("Update Tangem to the latest version to use Lending on "),
        provider.title,
        stringReference("."),
    )
}