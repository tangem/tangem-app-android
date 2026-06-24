package com.tangem.datasource.local.txhistory.db.entity.express

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Persisted onramp country, matched to a transaction by [code] == [ExpressOnrampEntity.countryCode]. */
@Entity(tableName = "onramp_country")
data class OnrampCountryEntity(

    @PrimaryKey
    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "image")
    val image: String,

    @ColumnInfo(name = "alpha3")
    val alpha3: String,

    @ColumnInfo(name = "continent")
    val continent: String,

    @ColumnInfo(name = "onramp_available")
    val isOnrampAvailable: Boolean,

    @Embedded(prefix = "currency_")
    val defaultCurrency: CurrencyEmbedded,
) {

    data class CurrencyEmbedded(

        @ColumnInfo(name = "name")
        val name: String,

        @ColumnInfo(name = "code")
        val code: String,

        @ColumnInfo(name = "image")
        val image: String?,

        @ColumnInfo(name = "precision")
        val precision: Int,

        @ColumnInfo(name = "unit")
        val unit: String,
    )
}