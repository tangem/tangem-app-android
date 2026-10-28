package com.tangem.features.marketing.impl.concierge.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference

/** Survey questions in display order. [key] and [Option.key] are backend keys, not localization keys. */
internal enum class ConciergeDemandSurveyQuestion(
    val key: String,
    val title: TextReference,
    val hint: TextReference?,
    val isOptional: Boolean,
    val options: List<Option>,
) {
    GOAL(
        key = "goal",
        title = stringReference(QuestionTexts.GOAL_QUESTION),
        hint = null,
        isOptional = false,
        options = listOf(
            Option(key = "buy", label = stringReference(QuestionTexts.GOAL_BUY)),
            Option(key = "sell", label = stringReference(QuestionTexts.GOAL_SELL)),
            Option(key = "buy_sell", label = stringReference(QuestionTexts.GOAL_BUY_SELL)),
        ),
    ),
    TRANSACTION_SIZE(
        key = "transaction_size",
        title = stringReference(QuestionTexts.TRANSACTION_SIZE_QUESTION),
        hint = stringReference(QuestionTexts.TRANSACTION_SIZE_HINT),
        isOptional = false,
        options = listOf(
            Option(key = "30k_500k", label = stringReference(QuestionTexts.TRANSACTION_SIZE_30K_500K)),
            Option(key = "500k_1m", label = stringReference(QuestionTexts.TRANSACTION_SIZE_500K_1M)),
            Option(key = "1m_plus", label = stringReference(QuestionTexts.TRANSACTION_SIZE_1M_PLUS)),
        ),
    ),
    FREQUENCY(
        key = "frequency",
        title = stringReference(QuestionTexts.FREQUENCY_QUESTION),
        hint = null,
        isOptional = false,
        options = listOf(
            Option(key = "one_off", label = stringReference(QuestionTexts.FREQUENCY_ONE_OFF)),
            Option(key = "few_times_year", label = stringReference(QuestionTexts.FREQUENCY_FEW_TIMES_YEAR)),
            Option(
                key = "few_times_month",
                label = stringReference(QuestionTexts.FREQUENCY_FEW_TIMES_MONTH),
            ),
            Option(key = "weekly_or_more", label = stringReference(QuestionTexts.FREQUENCY_WEEKLY_OR_MORE)),
        ),
    ),
    NEEDED_CURRENCY(
        key = "needed_currency",
        title = stringReference(QuestionTexts.NEEDED_CURRENCY_QUESTION),
        hint = stringReference(QuestionTexts.NEEDED_CURRENCY_HINT),
        isOptional = false,
        options = listOf(
            Option(key = "local", label = stringReference(QuestionTexts.NEEDED_CURRENCY_LOCAL)),
            Option(key = "usd", label = stringReference(QuestionTexts.NEEDED_CURRENCY_USD)),
            Option(key = "eur", label = stringReference(QuestionTexts.NEEDED_CURRENCY_EUR)),
            Option(key = "other", label = stringReference(QuestionTexts.NEEDED_CURRENCY_OTHER)),
        ),
    ),
    CURRENT_SOLUTION(
        key = "current_solution",
        title = stringReference(QuestionTexts.CURRENT_SOLUTION_QUESTION),
        hint = null,
        isOptional = true,
        options = listOf(
            Option(key = "exchange", label = stringReference(QuestionTexts.CURRENT_SOLUTION_EXCHANGE)),
            Option(key = "otc_desk", label = stringReference(QuestionTexts.CURRENT_SOLUTION_OTC_DESK)),
            Option(key = "p2p", label = stringReference(QuestionTexts.CURRENT_SOLUTION_P2P)),
            Option(
                key = "cash_exchange",
                label = stringReference(QuestionTexts.CURRENT_SOLUTION_CASH_EXCHANGE),
            ),
        ),
    ),
    FUNDS_OWNER(
        key = "funds_owner",
        title = stringReference(QuestionTexts.FUNDS_OWNER_QUESTION),
        hint = null,
        isOptional = true,
        options = listOf(
            Option(key = "own_only", label = stringReference(QuestionTexts.FUNDS_OWNER_OWN_ONLY)),
            Option(key = "third_party", label = stringReference(QuestionTexts.FUNDS_OWNER_THIRD_PARTY)),
        ),
    ),
    BUSINESS_INTEREST(
        key = "business_interest",
        title = stringReference(QuestionTexts.BUSINESS_INTEREST_QUESTION),
        hint = null,
        isOptional = true,
        options = listOf(
            Option(key = "yes", label = resourceReference(R.string.common_yes)),
            Option(key = "no", label = resourceReference(R.string.common_no)),
        ),
    ),
    ;

    data class Option(val key: String, val label: TextReference)
}

// TODO [REDACTED_TASK_KEY] Replace the hardcoded texts with the survey_demand_* Lokalise keys
private object QuestionTexts {
    const val GOAL_QUESTION = "What can we help you with?"
    const val GOAL_BUY = "Buy crypto"
    const val GOAL_SELL = "Sell crypto"
    const val GOAL_BUY_SELL = "Buy and sell"
    const val TRANSACTION_SIZE_QUESTION = "What’s a typical amount per transaction?"
    const val TRANSACTION_SIZE_HINT = "Roughly, in USD"
    const val TRANSACTION_SIZE_30K_500K = "$30K–500K"
    const val TRANSACTION_SIZE_500K_1M = "$500K–1M"
    const val TRANSACTION_SIZE_1M_PLUS = "$1M+"
    const val FREQUENCY_QUESTION = "How often are you planning to buy or sell?"
    const val FREQUENCY_ONE_OFF = "Just once"
    const val FREQUENCY_FEW_TIMES_YEAR = "A few times a year"
    const val FREQUENCY_FEW_TIMES_MONTH = "A few times a month"
    const val FREQUENCY_WEEKLY_OR_MORE = "Weekly or more"
    const val NEEDED_CURRENCY_QUESTION = "Which currency would you like to use?"
    const val NEEDED_CURRENCY_HINT = "Pick the main one"
    const val NEEDED_CURRENCY_LOCAL = "My country’s currency"
    const val NEEDED_CURRENCY_USD = "US dollar"
    const val NEEDED_CURRENCY_EUR = "Euro"
    const val NEEDED_CURRENCY_OTHER = "Another currency"
    const val CURRENT_SOLUTION_QUESTION = "Where do you buy and sell crypto now?"
    const val CURRENT_SOLUTION_EXCHANGE = "Crypto exchange"
    const val CURRENT_SOLUTION_OTC_DESK = "OTC desk"
    const val CURRENT_SOLUTION_P2P = "P2P"
    const val CURRENT_SOLUTION_CASH_EXCHANGE = "Cash exchange"
    const val FUNDS_OWNER_QUESTION = "Whose funds are involved?"
    const val FUNDS_OWNER_OWN_ONLY = "Only my own"
    const val FUNDS_OWNER_THIRD_PARTY = "Someone else’s too"
    const val BUSINESS_INTEREST_QUESTION = "Are you interested in OTC services for your business?"
}