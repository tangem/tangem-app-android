package com.tangem.datasource.local.token

import com.squareup.moshi.Moshi
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.files.FileReader
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runner that launch migrations of saving user tokens store
 *
 * @property appPreferencesStore application preference store
 * @property fileReader          file reader
 * @property moshi               moshi
 * @property dispatchers         dispatchers
 *
* [REDACTED_AUTHOR]
 */
@Singleton
class UserTokensStoreMigrationRunner @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
    private val fileReader: FileReader,
    @NetworkMoshi private val moshi: Moshi,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend fun run(ids: List<String>) {
        ids.forEach { id ->
            coroutineScope { run(id) }
        }
    }

    private suspend fun run(id: String) {
        withContext(dispatchers.io) {
            val migration = UserTokensStoreMigration(
                userWalletId = id,
                moshi = moshi,
                fileReader = fileReader,
            )

            migration.migrate(appPreferencesStore)

            migration.cleanUp()
        }
    }
}
