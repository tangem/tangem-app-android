package com.tangem.common.ui.amountScreen.models

import androidx.compose.runtime.Stable
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.PersistentList

/**
 * Model for amount state
 *
 * @param isPrimaryButtonEnabled indicates if next state button enabled
 * @param walletName user wallet name
 * @param walletBalance user crypto currency balance in wallet
 * @param tokenIconState crypto currency icon state
 * @param segmentedButtonConfig currency switcher config
 * @param selectedButton selected currency index
 * @param isSegmentedButtonsEnabled indicates if currency switches is enabled
 * @param amountTextField amount field state
 * @param appCurrencyCode app currency code
 */
@Stable
data class AmountState(
    val isPrimaryButtonEnabled: Boolean,
    val walletName: String,
    val walletBalance: TextReference,
    val tokenIconState: TokenIconState,
    val segmentedButtonConfig: PersistentList<AmountSegmentedButtonsConfig>,
    val selectedButton: Int,
    val isSegmentedButtonsEnabled: Boolean,
    val amountTextField: AmountFieldModel,
    val appCurrencyCode: String,
)