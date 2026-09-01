package com.tangem.datasource.local.txhistory.db.entity.express

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted onramp fiat currency, matched to a transaction by [code] == [ExpressOnrampEntity.fromCurrencyCode].
 */
@Entity(tableName = "onramp_currency")
data class OnrampCurrencyEntity(

    @PrimaryKey
    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "image")
    val image: String?,

    @ColumnInfo(name = "precision")
    val precision: Int,

    @ColumnInfo(name = "unit")
    val unit: String,
)