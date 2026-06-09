package com.tangem.features.txhistory.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

/**
 * UI model for the in-app transaction details ("Operation") card.
 *
 * One model for all transaction types; the layout family is chosen from the transaction type by
 * `TxInfoToTxHistoryDetailsUMConverter`:
 * - [SingleAsset] — Receive / Send / Transfer
 * - [TwoAssets] — Swap / Onramp
 */
@Immutable
internal sealed interface TxHistoryDetailsUM : TangemBottomSheetConfigContent {

    /** Operation title, status-driven color is resolved at render time. */
    val title: String

    /** Single-asset layout: Receive / Send / Transfer */
    data class SingleAsset(
        override val title: String,
    ) : TxHistoryDetailsUM

    /** Two-asset layout: Swap / Onramp */
    data class TwoAssets(
        override val title: String,
    ) : TxHistoryDetailsUM
}