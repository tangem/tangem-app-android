package com.tangem.datasource.asset.reader

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStream
import javax.inject.Inject

/**
 * Implementation of asset file reader
 *
 * @property context application context
 */
internal class AndroidAssetReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : AssetReader {

    override fun readJson(fileName: String): String {
        return openFile("$fileName.json")
            .bufferedReader()
            .use(BufferedReader::readText)
    }

    override fun openFile(file: String): InputStream {
        return context.assets.open(file)
    }
}
