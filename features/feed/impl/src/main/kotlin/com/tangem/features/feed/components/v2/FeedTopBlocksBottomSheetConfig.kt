package com.tangem.features.feed.components.v2

/** Not serializable on purpose: the sheet is a step of a decision the model is still taking. */
internal sealed interface FeedTopBlocksBottomSheetConfig {

    /** Asks which wallet to open collectibles for, when several are eligible. */
    data object WalletSelector : FeedTopBlocksBottomSheetConfig
}