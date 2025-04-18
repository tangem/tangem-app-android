package com.tangem.datasource.asset.reader

import android.content.res.AssetManager
import java.io.BufferedReader

/**
 * Implementation of asset file reader
 *
 * @property assetManager asset manager
 */
internal class AndroidAssetReader(
    private val assetManager: AssetManager,
) : AssetReader {

    override suspend fun read(fullFileName: String): String {
        return assetManager.open(fullFileName).bufferedReader()
            .use(BufferedReader::readText)
    }
}