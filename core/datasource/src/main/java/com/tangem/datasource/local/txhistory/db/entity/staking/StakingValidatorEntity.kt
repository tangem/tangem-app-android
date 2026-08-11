package com.tangem.datasource.local.txhistory.db.entity.staking

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted representation of a single StakeKit validator.
 *
 * Mirrors [com.tangem.datasource.api.stakekit.models.response.model.YieldDTO.ValidatorDTO].
 */
@Entity(tableName = "staking_validator")
data class StakingValidatorEntity(

    @PrimaryKey
    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "status")
    val status: String?,

    @ColumnInfo(name = "name")
    val name: String?,

    @ColumnInfo(name = "image")
    val image: String?,

    @ColumnInfo(name = "website")
    val website: String?,

    @ColumnInfo(name = "apr")
    val apr: String?,

    @ColumnInfo(name = "commission")
    val commission: Double?,

    @ColumnInfo(name = "staked_balance")
    val stakedBalance: String?,

    @ColumnInfo(name = "voting_power")
    val votingPower: Double?,

    @ColumnInfo(name = "preferred")
    val isPreferred: Boolean?,
)