package com.tangem.domain.assetsdiscovery.model

sealed class AssetsDiscoveryProgress {

    data object Idle : AssetsDiscoveryProgress()

    data class InProgress(
        val completedNetworks: Int,
        val totalNetworks: Int,
    ) : AssetsDiscoveryProgress() {
        val progressPercent: Int
            get() = if (totalNetworks > 0) {
                completedNetworks * 100 / totalNetworks
            } else {
                0
            }
    }

    data object Completed : AssetsDiscoveryProgress()
}