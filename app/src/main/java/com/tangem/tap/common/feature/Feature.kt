package com.tangem.tap.common.feature

/**
 * Created by Anton Zhilenkov on 19/08/2022.
 */
interface Feature {
    fun featureIsSwitchedOn(): Boolean
}
