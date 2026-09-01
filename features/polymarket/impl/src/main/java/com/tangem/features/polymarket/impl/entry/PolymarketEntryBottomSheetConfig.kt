package com.tangem.features.polymarket.impl.entry

/**
 * Which bottom sheet the entry route is showing.
 *
 * Deliberately not serializable: the sheet is a step of a decision the model is in the middle of taking, and
 * restoring it after process death would put the user back on a question the flow has already moved past.
 */
internal sealed interface PolymarketEntryBottomSheetConfig {

    /** Asks which wallet to onboard, when the caller supplied none and several are eligible. */
    data object WalletSelector : PolymarketEntryBottomSheetConfig
}