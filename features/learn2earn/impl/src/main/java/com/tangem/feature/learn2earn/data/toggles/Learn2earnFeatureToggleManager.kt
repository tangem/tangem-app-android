package com.tangem.feature.learn2earn.data.toggles

/**
 * Feature toggles manager implementation of "learn2earn" feature
 *
 * @author Anton Zhilenkov on 03.07.2023.
 */
interface Learn2earnFeatureToggleManager {

    val isLearn2earnEnabled: Boolean
}
