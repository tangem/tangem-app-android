package com.tangem.domain.hotwallet

class IsAccessCodeSimpleUseCase {
    operator fun invoke(accessCode: String): Boolean {
        return isSequential(accessCode) || isRepeatedCharacters(accessCode)
    }

    fun isSequential(code: String): Boolean = code.length > 1 &&
        (code.zipWithNext().all { it.second == it.first + 1 } ||
            code.zipWithNext().all { it.second == it.first - 1 })

    fun isRepeatedCharacters(code: String): Boolean = code.isNotEmpty() && code.all { it == code.first() }
}