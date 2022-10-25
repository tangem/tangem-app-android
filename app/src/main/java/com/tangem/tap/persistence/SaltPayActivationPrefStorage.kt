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
interface SaltPayActivationStorage {
    var data: SaltPayActivationData

    fun reset()

    companion object {
        fun stub(): SaltPayActivationStorage = object : SaltPayActivationStorage {
            override var data: SaltPayActivationData = SaltPayActivationData()
            override fun reset() {}
        }
    }
}

data class SaltPayActivationData(
    val transactionsSent: Boolean = false,
)

class SaltPayActivationPrefStorage(
    context: Context,
    private val converter: MoshiJsonConverter,
) : SaltPayActivationStorage {

    private val storage: ArmadilloSharedPreferences = SecureStorage.createEncryptedSharedPreferences(
        context = context,
        storageName = "saltpay",
    )

    private var isFetching: Boolean = false

    init {
        fetch()
    }

    override var data: SaltPayActivationData = SaltPayActivationData()
        set(value) {
            field = value
            if (!isFetching) save()
        }

    override fun reset() {
        storage.edit(true) {
            this.remove(REGISTRATION_DATA_KEY)
            data = SaltPayActivationData()
        }
    }

    private fun save() {
        storage.edit(true) {
            putString(REGISTRATION_DATA_KEY, converter.toJson(data))
        }
    }

    private fun fetch(): SaltPayActivationData {
        isFetching = true

        val jsonData = storage.getString(REGISTRATION_DATA_KEY, null)
        data = if (jsonData == null) {
            SaltPayActivationData()
        } else {
            converter.fromJson<SaltPayActivationData>(jsonData) ?: SaltPayActivationData()
        }
        isFetching = false

        return data
    }

    companion object {
        private const val REGISTRATION_DATA_KEY = "saltpay_registration_data"
    }
}
