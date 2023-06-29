package com.tangem.feature.learn2earn.domain.models

import com.tangem.datasource.api.promotion.models.AbstractPromotionResponse

/**
[REDACTED_AUTHOR]
 */
sealed class PromotionError(val code: Int, val description: String) : Throwable(
    message = "code: $code, description: $description",
) {

    object NetworkUnreachable : PromotionError(code = 0, "Network or service not available")

    class CodeNotFound(code: Int, description: String) : PromotionError(code, description)
    class CodeWasNotAppliedInShop(code: Int, description: String) : PromotionError(code, description)
    class CodeWasAlreadyUsed(code: Int, description: String) : PromotionError(code, description)
    class WalletAlreadyHasAward(code: Int, description: String) : PromotionError(code, description)
    class CardAlreadyHasAward(code: Int, description: String) : PromotionError(code, description)
    class ProgramNotFound(code: Int, description: String) : PromotionError(code, description)
    class ProgramWasEnd(code: Int, description: String) : PromotionError(code, description)

    class UnknownError(description: String) : PromotionError(code = -1, description)
}

@Suppress("MagicNumber")
internal fun AbstractPromotionResponse.Error.toDomainError(): PromotionError = when (code) {
    101 -> PromotionError.CodeNotFound(code, message)
    102 -> PromotionError.CodeWasNotAppliedInShop(code, message)
    103 -> PromotionError.CodeWasAlreadyUsed(code, message)
    104 -> PromotionError.WalletAlreadyHasAward(code, message)
    105 -> PromotionError.CardAlreadyHasAward(code, message)
    106 -> PromotionError.ProgramNotFound(code, message)
    107 -> PromotionError.ProgramWasEnd(code, message)
    else -> PromotionError.UnknownError("$code:$message")
}