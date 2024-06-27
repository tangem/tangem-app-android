package com.tangem.datasource.asset.reader

import android.content.res.AssetManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.io.BufferedReader

/**
 * Implementation of asset file reader
 *
 * @property assetManager asset manager
 * @property dispatchers  dispatchers
 */
internal class AndroidAssetReader(
    private val assetManager: AssetManager,
    private val dispatchers: CoroutineDispatcherProvider,
) : AssetReader {

    override suspend fun read(fullFileName: String): String = withContext(dispatchers.io) {
        assetManager.open(fullFileName).bufferedReader()
            .use(BufferedReader::readText)
    }
}