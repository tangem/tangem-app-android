package com.tangem.datasource.local.token

import androidx.datastore.core.DataMigration
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.files.FileReader
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject

/**
 * Migration of saving [UserTokensResponse] from file to [AppPreferencesStore]
 *
 * @param userWalletId  user wallet id
 * @param moshi         moshi
 * @property fileReader file reader
 *
* [REDACTED_AUTHOR]
 */
internal class UserTokensStoreMigration(
    userWalletId: String,
    moshi: Moshi,
    private val fileReader: FileReader,
) : DataMigration<AppPreferencesStore> {

    private val legacyFileName = "user_tokens_$userWalletId"
    private val keyName = PreferencesKeys.getUserTokensKey(userWalletId = userWalletId)

    @OptIn(ExperimentalStdlibApi::class)
    private val adapter = moshi.adapter<UserTokensResponse>()

    override suspend fun shouldMigrate(currentData: AppPreferencesStore): Boolean = true

    override suspend fun migrate(currentData: AppPreferencesStore): AppPreferencesStore {
        val currentKey = currentData.getObjectSyncOrNull<UserTokensResponse>(key = keyName)

        if (currentKey != null) return currentData

        val value = runCatching {
            val json = fileReader.readFile(legacyFileName)
            adapter.fromJson(json)
        }.getOrNull()

        if (value != null) {
            currentData.storeObject(key = keyName, value = value)
        }

        return currentData
    }

    override suspend fun cleanUp() {
        fileReader.removeFile(legacyFileName)
    }
}
