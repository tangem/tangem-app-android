# Transaction history — domain

The models and contracts of this module (`TxHistoryInfo`, `HistoryTxListManager`,
`TxHistoryFetchTrigger`, `TxHistoryRepositoryV2`) are documented together with the pipeline that
produces and consumes them.

**Read `data/txhistory/CLAUDE.md` before changing anything here.** It is the single doc for the whole
transaction history: loading (the fetcher tree, the background sync), reading (source selection, the
`history_index`, the merge), the invariants of `TxHistoryInfo` and the `HistoryTxListManager`
contract — plus the checklists for extending any of it.