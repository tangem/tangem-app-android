# Transaction history — feature

This module renders the timeline the data layer produces: `TxHistoryModel` collects
`HistoryTxListManager.state` and converts each `TxHistoryInfo` into a `TxHistoryItemsUM` row.

**Read `data/txhistory/CLAUDE.md` before changing anything here.** It is the single doc for the whole
transaction history: where the rows come from (the fetcher tree and the background sync), how the
timeline is assembled (source selection, the `history_index`, the merge), and what each
`TxHistoryInfo` subtype guarantees — `txId`, `timestampMillis`, `explorerHash`, `idToCopy`.

The UI mapping itself (part 3 of that doc) is not documented yet.