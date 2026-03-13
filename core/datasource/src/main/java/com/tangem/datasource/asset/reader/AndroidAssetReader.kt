package com.tangem.datasource.asset.reader

import android.content.res.AssetManager

/**
 * Implementation of asset file reader
 *
 * @property assetManager asset manager
 */
internal class AndroidAssetReader(
    private val assetManager: AssetManager,
) : AssetReader {

    override suspend fun read(fullFileName: String): String {
        return assetManager.open(fullFileName, AssetManager.ACCESS_BUFFER).use { inputStream ->
            inputStream.readBytes().toString(Charsets.UTF_8)
        }
    }
}