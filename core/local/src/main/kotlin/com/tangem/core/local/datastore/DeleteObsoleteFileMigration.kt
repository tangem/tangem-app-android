package com.tangem.core.local.datastore

import androidx.datastore.core.DataMigration
import com.tangem.utils.logging.TangemLogger
import java.io.File

class DeleteObsoleteFileMigration<T>(produceFile: () -> File) : DataMigration<T> {

    private val file: File by lazy(produceFile)

    override suspend fun shouldMigrate(currentData: T): Boolean = file.exists()

    override suspend fun migrate(currentData: T): T = currentData

    override suspend fun cleanUp() {
        val isDeleted = runCatching { file.delete() }.getOrDefault(false)
        if (isDeleted) {
            TangemLogger.i("Obsolete DataStore file ${file.name} deleted")
        } else {
            TangemLogger.e("Obsolete DataStore file ${file.name} cannot be deleted")
        }
    }
}