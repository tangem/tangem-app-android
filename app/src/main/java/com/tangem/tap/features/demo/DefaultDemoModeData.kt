package com.tangem.tap.features.demo

import com.tangem.datasource.demo.DemoModeData
import com.tangem.tap.proxy.AppStateHolder
import javax.inject.Inject

class DefaultDemoModeData @Inject constructor(private val appStateHolder: AppStateHolder) : DemoModeData {

    override val isDemoModeActive: Boolean
        get() = appStateHolder.scanResponse?.let { DemoHelper.isDemoCard(it) } == true
}