package com.tangem.features.polymarket.impl.entry

/**
 * Which bottom sheet the entry route is showing. The two are never open at once — a wallet is picked before
 * its deposit network is checked — so they share one slot.
 *
 * Deliberately not serializable: [AddDepositNetwork] renders against an `AddToPortfolioManager` built at
 * activation time and held by the model, which does not survive process death. Restoring the sheet without it
 * would fail, so the slot is left unrestored instead.
 */
internal sealed interface PolymarketEntryBottomSheetConfig {

    /** Asks which wallet to onboard, when the caller supplied none and several are eligible. */
    data object WalletSelector : PolymarketEntryBottomSheetConfig

    /** Offers to add the deposit chain to the settled wallet, when that wallet does not hold it. */
    data object AddDepositNetwork : PolymarketEntryBottomSheetConfig
}