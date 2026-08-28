package com.tangem.datasource.local.files

import android.content.Context
import com.tangem.core.local.files.FileReader
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class AndroidFileReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatcherProvider,
) : FileReader {

    override suspend fun readFile(fileName: String): String = withContext(dispatchers.io) {
        context.openFileInput(fileName).use { stream ->
            stream.bufferedReader().use { reader ->
                reader.readText()
            }
        }
    }

    override suspend fun rewriteFile(content: String, fileName: String) {
        withContext(dispatchers.io) {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { stream ->
                stream.writer().use { writer ->
                    // warning: don't write byteArray as JSON — it breaks Cyrillic encoding
                    writer.write(content)
                }
            }
        }
    }

    override suspend fun removeFile(fileName: String) {
        withContext(dispatchers.io) {
            context.deleteFile(fileName)
        }
    }
}