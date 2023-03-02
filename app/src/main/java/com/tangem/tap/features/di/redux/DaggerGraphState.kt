package com.tangem.tap.features.di.redux

import com.tangem.datasource.asset.AssetReader
import org.rekotlin.StateType

data class DaggerGraphState(val assetReader: AssetReader? = null) : StateType