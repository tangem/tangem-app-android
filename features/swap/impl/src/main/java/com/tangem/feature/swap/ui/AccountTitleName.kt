package com.tangem.feature.swap.ui

import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.Account
import com.tangem.feature.swap.presentation.R

internal fun Account.toTitleName(): TextReference = when (this) {
    is Account.Prediction -> resourceReference(R.string.prediction_account_title)
    else -> accountName.toUM().value
}