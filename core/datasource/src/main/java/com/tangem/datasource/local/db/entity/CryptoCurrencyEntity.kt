package com.tangem.datasource.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = CryptoCurrenciesAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UserWalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["userWalletId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["currencyBackendId"]),
        Index(value = ["networkId"]),
        Index(value = ["accountId"]),
        Index(value = ["userWalletId"]),
    ],
)
data class CryptoCurrencyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val currencyBackendId: String?,
    val networkId: String,
    val accountId: Int,
    val userWalletId: String,
    val name: String,
    val symbol: String,
    val decimals: Int,
    val contractAddress: String?,
    val derivationPath: String?,
)