package com.tangem.datasource.local.assets

import android.content.Context
import android.content.res.AssetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Reads asset files bundled with the app. */
internal class AssetReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun read(fullFileName: String): String {
        return context.assets.open(fullFileName, AssetManager.ACCESS_BUFFER).use { inputStream ->
            inputStream.readBytes().toString(Charsets.UTF_8)
        }
    }
}