package com.tangem.datasource.local.txhistory.db.entity.express

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "express_provider",
)
data class ExpressProviderEntity(

    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "icon_url")
    val iconUrl: String,

    @ColumnInfo(name = "provider_url")
    val providerUrl: String,
)