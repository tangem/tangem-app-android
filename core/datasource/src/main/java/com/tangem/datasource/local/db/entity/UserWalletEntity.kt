package com.tangem.datasource.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["ordinalNumber"], unique = true),
    ],
)
data class UserWalletEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val artworkUrl: String,
    val isMultiCurrency: Boolean,
    val hasBackupError: Boolean,
    val cardsInWallet: Set<String>,
    val ordinalNumber: Int,
)