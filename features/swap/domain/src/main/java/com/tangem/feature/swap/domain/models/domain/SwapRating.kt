package com.tangem.feature.swap.domain.models.domain

/** Rating of an express swap */
sealed interface SwapRating {

    data object NotRated : SwapRating

    data class Rated(val rating: Int) : SwapRating
}