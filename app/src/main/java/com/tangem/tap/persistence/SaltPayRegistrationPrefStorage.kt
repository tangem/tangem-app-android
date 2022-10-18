package com.tangem.tap.persistence

import android.content.Context
import androidx.core.content.edit
import at.favre.lib.armadillo.ArmadilloSharedPreferences
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.common.services.secure.SecureStorage
import com.tangem.tangem_sdk_new.storage.createEncryptedSharedPreferences

/**
* [REDACTED_AUTHOR]
 */
interface SaltPayRegistrationStorage {
    var data: SaltPayRegistratorData

    fun reset()

    companion object {
        fun stub(): SaltPayRegistrationStorage = object : SaltPayRegistrationStorage {
            override var data: SaltPayRegistratorData = SaltPayRegistratorData()
            override fun reset() {}
        }
    }
}

data class SaltPayRegistratorData(
    val transactionsSent: Boolean = false,
)

class SaltPayRegistrationPrefStorage(
    context: Context,
    private val converter: MoshiJsonConverter,
) : SaltPayRegistrationStorage {

    private val storage: ArmadilloSharedPreferences = SecureStorage.createEncryptedSharedPreferences(
        context = context,
        storageName = "saltpay",
    )

    private var isFetching: Boolean = false

    init {
        fetch()
    }

    override var data: SaltPayRegistratorData = SaltPayRegistratorData()
        set(value) {
            field = value
            if (!isFetching) save()
        }

    override fun reset() {
        storage.edit(true) {
            this.remove(REGISTRATION_DATA_KEY)
            data = SaltPayRegistratorData()
        }
    }

    private fun save() {
        storage.edit(true) {
            putString(REGISTRATION_DATA_KEY, converter.toJson(data))
        }
    }

    private fun fetch(): SaltPayRegistratorData {
        isFetching = true

        val jsonData = storage.getString(REGISTRATION_DATA_KEY, null)
        data = if (jsonData == null) {
            SaltPayRegistratorData()
        } else {
            converter.fromJson<SaltPayRegistratorData>(jsonData) ?: SaltPayRegistratorData()
        }
        isFetching = false

        return data
    }

    companion object {
        private const val REGISTRATION_DATA_KEY = "saltpay_registration_data"
    }
}
