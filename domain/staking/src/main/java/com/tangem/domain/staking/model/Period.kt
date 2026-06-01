package com.tangem.domain.staking.model

sealed class Period {

    abstract val value: Int

    data class Days(
        override val value: Int,
    ) : Period()

    data class Seconds(
        override val value: Int,
    ) : Period()
}