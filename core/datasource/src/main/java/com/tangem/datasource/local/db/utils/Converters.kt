package com.tangem.datasource.local.db.utils

import androidx.room.TypeConverter
import com.tangem.domain.models.wallet.UserWalletId

internal class Converters {

    @TypeConverter
    fun setFromString(value: String): Set<String> {
        return value.split(",").toSet()
    }

    @TypeConverter
    fun setToString(set: Set<Any>): String {
        return set.joinToString(",")
    }

    @TypeConverter
    fun userWalletIdFromString(value: String): UserWalletId {
        return UserWalletId(value)
    }

    @TypeConverter
    fun userWalletIdToString(userWalletId: UserWalletId): String {
        return userWalletId.stringValue
    }
}