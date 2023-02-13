package com.tangem.datasource.utils

import android.content.Context

/**
[REDACTED_AUTHOR]
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