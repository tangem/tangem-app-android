package com.tangem.datasource.utils

import android.content.Context

/**
 * Created by Anton Zhilenkov on 15/09/2022.
 */
interface AssetReader {
    fun readAssetAsString(name: String): String
}

class AndroidAssetReader(
    private val context: Context,
) : AssetReader {

    override fun readAssetAsString(name: String): String {
        return context.readAssetAsString(name)
    }
}

fun Context.readAssetAsString(fileName: String): String {
    return this.assets.open("$fileName.json").bufferedReader().readText()
}
