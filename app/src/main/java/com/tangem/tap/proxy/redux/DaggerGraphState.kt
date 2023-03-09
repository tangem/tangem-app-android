package com.tangem.tap.proxy.redux

import com.tangem.datasource.asset.AssetReader
import com.tangem.features.tester.api.TesterRouter
import org.rekotlin.StateType

data class DaggerGraphState(
    val assetReader: AssetReader? = null,
    val testerRouter: TesterRouter? = null,
) : StateType
