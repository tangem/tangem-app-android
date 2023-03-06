package com.tangem.tap.features.di.redux

import com.tangem.datasource.asset.AssetReader
import org.rekotlin.Action

sealed interface DaggerGraphAction : Action {

    data class SetDependencies(val assetReader: AssetReader) : DaggerGraphAction
}