package com.tangem.datasource.files

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okio.use
import javax.inject.Inject

class AndroidFileReader @Inject constructor(@ApplicationContext private val context: Context) : FileReader {

    override fun readFile(fileName: String): String {
        return context.openFileInput(fileName).use { stream ->
            stream.bufferedReader().use { reader ->
                reader.readText()
            }
        }
    }

    override fun rewriteFile(content: String, fileName: String) {
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use { stream ->
            stream.writer().use { writer ->
                // warning: don't write byteArray as json, its break cyrillic encoding
                writer.write(content)
            }
        }
    }

    override fun removeFile(fileName: String) {
        context.deleteFile(fileName)
    }
}