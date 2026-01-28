package com.tangem.domain.staking.model

sealed class CooldownPeriod {

    data class Fixed(val days: Int) : CooldownPeriod()

    data class Range(val minDays: Int, val maxDays: Int) : CooldownPeriod()
}