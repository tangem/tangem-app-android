package com.tangem.blockchainsdk.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager

internal class DefaultBlockchainSDKFeatureToggles(
    @Suppress("UnusedPrivateMember") private val featureTogglesManager: FeatureTogglesManager,
) : BlockchainSDKFeatureToggles