package com.tangem.test.core.datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.datasource.utils.AppDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Minimal real [AppDataStoreFactory] for unit tests.
 *
 * The production implementation is internal, so tests build the contract directly. It creates DataStores that
 * fall back to the serializer's default value on corruption, without the production analytics reporting.
 *
 * @param filesDir base directory the [fileName] overload resolves files against (the production factory uses the
 *   app context's DataStore directory).
 * @param appScope scope used by the [KSerializer] overload (the production factory injects the app scope there).
 * @param json config used by the [KSerializer] overload (the production factory injects `@NetworkJson` there).
 * @param isKotlinxEnabled value of the kotlinx DataStore toggle; while off, the [KSerializer] overload keeps
 *   the default kotlinx config, as the production factory does.
 */
fun createAppDataStoreFactory(
    filesDir: File = File(System.getProperty("java.io.tmpdir")),
    appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    json: Json = NetworkJsonFactory.create(),
    isKotlinxEnabled: Boolean = true,
): AppDataStoreFactory = object : AppDataStoreFactory {

    override val isKotlinxDataStoreEnabled: Boolean = isKotlinxEnabled

    override fun <T> create(
        serializer: Serializer<T>,
        scope: CoroutineScope,
        produceFile: () -> File,
        migrations: List<DataMigration<T>>,
    ): DataStore<T> {
        val file = produceFile()
        return DataStoreFactory.create(
            serializer = serializer,
            corruptionHandler = ReplaceFileCorruptionHandler { serializer.defaultValue },
            migrations = migrations,
            scope = scope,
            produceFile = { file },
        )
    }

    override fun <T> create(defaultValue: T, serializer: KSerializer<T>, fileName: String): DataStore<T> {
        return create(
            serializer = if (isKotlinxDataStoreEnabled) {
                KotlinxDataStoreSerializer(defaultValue = defaultValue, serializer = serializer, json = json)
            } else {
                KotlinxDataStoreSerializer(defaultValue = defaultValue, serializer = serializer)
            },
            scope = appScope,
            produceFile = { File(filesDir, fileName) },
        )
    }
}