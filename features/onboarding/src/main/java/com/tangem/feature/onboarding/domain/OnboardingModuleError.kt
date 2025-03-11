package com.tangem.feature.onboarding.domain

import com.tangem.common.module.ModuleError
import com.tangem.common.module.ModuleErrorCode
import com.tangem.common.module.ModuleMessage

/**
[REDACTED_AUTHOR]
 * For each pool of errors, use the codes lying in range 100 (200..299, 300..399, etc)
 */
abstract class OnboardingModuleError(
    subCode: Int,
    override val message: String,
    override val data: Any?,
) : ModuleMessage, ModuleError() {
    override val code: Int = ModuleErrorCode.ONBOARDING + subCode

    companion object {
        internal const val ERROR_CODE_SEED_PHRASE = 100
    }
}

sealed class SeedPhraseError(
    subCode: Int = 0,
    message: String? = null,
    data: Any? = null,
) : OnboardingModuleError(
    subCode = ERROR_CODE_SEED_PHRASE + subCode,
    message = message ?: this::class.java.simpleName,
    data = data,
) {
    object InvalidWordCount : SeedPhraseError(subCode = 0)
    object InvalidEntropyLength : SeedPhraseError(subCode = 1)
    object InvalidWordsFile : SeedPhraseError(subCode = 2)
    object InvalidChecksum : SeedPhraseError(subCode = 3)
    object MnenmonicCreationFailed : SeedPhraseError(subCode = 4)
    object NormalizationFailed : SeedPhraseError(subCode = 5)
    object UnsupportedLanguage : SeedPhraseError(subCode = 6)
    data class InvalidWords(val words: Set<String>) : SeedPhraseError(subCode = 7)
    object InvalidMnemonic : SeedPhraseError(subCode = 8)
}