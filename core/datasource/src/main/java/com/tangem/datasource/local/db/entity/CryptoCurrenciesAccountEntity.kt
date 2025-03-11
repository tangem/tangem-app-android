package com.tangem.datasource.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = UserWalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["userWalletId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["id"]),
        Index(value = ["userWalletId"]),
    ],
)
data class CryptoCurrenciesAccountEntity(
    @PrimaryKey
    val id: Int,
    val userWalletId: String,
    val title: String,
    val currenciesCount: Int,
    val isArchived: Boolean,
    val ordinalNumber: Int,
)