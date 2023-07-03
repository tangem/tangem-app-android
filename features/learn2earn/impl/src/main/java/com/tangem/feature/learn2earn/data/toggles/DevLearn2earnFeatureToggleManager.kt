package com.tangem.feature.learn2earn.data.toggles

/**
 * @author Anton Zhilenkov on 03.07.2023.
 */
class DevLearn2earnFeatureToggleManager : Learn2earnFeatureToggleManager {

    override val isLearn2earnEnabled: Boolean
        get() = true
}
