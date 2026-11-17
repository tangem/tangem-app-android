package com.tangem.features.collectibles.impl.inventory.model

/** What started a refresh: it decides where the progress shows, the pull indicator or the reload button. */
internal enum class CollectiblesInventoryRefreshTrigger {
    PullToRefresh,
    Reload,
}