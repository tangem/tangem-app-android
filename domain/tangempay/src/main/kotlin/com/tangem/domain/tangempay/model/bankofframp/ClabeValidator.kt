package com.tangem.domain.tangempay.model.bankofframp

object ClabeValidator {

    private const val CLABE_LENGTH = 18
    private const val RADIX = 10
    private val WEIGHTS = intArrayOf(3, 7, 1)

    fun isValid(clabe: String): Boolean {
        if (clabe.length != CLABE_LENGTH || clabe.any { it !in '0'..'9' }) return false
        val digits = clabe.map { it - '0' }
        val weightedSum = digits.dropLast(1).withIndex().sumOf { (index, digit) ->
            digit * WEIGHTS[index % WEIGHTS.size] % RADIX
        }
        return digits.last() == (RADIX - weightedSum % RADIX) % RADIX
    }
}