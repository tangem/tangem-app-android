package com.tangem.tap.features.demo

import com.tangem.datasource.demo.DemoModeDatasource
import com.tangem.tap.proxy.AppStateHolder
import javax.inject.Inject

class DefaultDemoModeDatasource @Inject constructor(private val appStateHolder: AppStateHolder) : DemoModeDatasource {

    override val isDemoModeActive: Boolean
        get() = appStateHolder.scanResponse?.let { DemoHelper.isDemoCard(it) } == true
}
