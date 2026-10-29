package com.tangem.features.marketing.impl.concierge.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference

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
        title = resourceReference(R.string.survey_demand_goal_question),
        hint = null,
        isOptional = false,
        options = listOf(
            Option(key = "buy", label = resourceReference(R.string.survey_demand_goal_buy)),
            Option(key = "sell", label = resourceReference(R.string.survey_demand_goal_sell)),
            Option(key = "buy_sell", label = resourceReference(R.string.survey_demand_goal_buy_sell)),
        ),
    ),
    TRANSACTION_SIZE(
        key = "transaction_size",
        title = resourceReference(R.string.survey_demand_transaction_size_question),
        hint = resourceReference(R.string.survey_demand_transaction_size_hint),
        isOptional = false,
        options = listOf(
            Option(key = "30k_500k", label = resourceReference(R.string.survey_demand_transaction_size_30k_500k)),
            Option(key = "500k_1m", label = resourceReference(R.string.survey_demand_transaction_size_500k_1m)),
            Option(key = "1m_plus", label = resourceReference(R.string.survey_demand_transaction_size_1m_plus)),
        ),
    ),
    FREQUENCY(
        key = "frequency",
        title = resourceReference(R.string.survey_demand_frequency_question),
        hint = null,
        isOptional = false,
        options = listOf(
            Option(key = "one_off", label = resourceReference(R.string.survey_demand_frequency_one_off)),
            Option(key = "few_times_year", label = resourceReference(R.string.survey_demand_frequency_few_times_year)),
            Option(
                key = "few_times_month",
                label = resourceReference(R.string.survey_demand_frequency_few_times_month),
            ),
            Option(key = "weekly_or_more", label = resourceReference(R.string.survey_demand_frequency_weekly_or_more)),
        ),
    ),
    NEEDED_CURRENCY(
        key = "needed_currency",
        title = resourceReference(R.string.survey_demand_needed_currency_question),
        hint = resourceReference(R.string.survey_demand_needed_currency_hint),
        isOptional = false,
        options = listOf(
            Option(key = "local", label = resourceReference(R.string.survey_demand_needed_currency_local)),
            Option(key = "usd", label = resourceReference(R.string.survey_demand_needed_currency_usd)),
            Option(key = "eur", label = resourceReference(R.string.survey_demand_needed_currency_eur)),
            Option(key = "other", label = resourceReference(R.string.survey_demand_needed_currency_other)),
        ),
    ),
    CURRENT_SOLUTION(
        key = "current_solution",
        title = resourceReference(R.string.survey_demand_current_solution_question),
        hint = null,
        isOptional = true,
        options = listOf(
            Option(key = "exchange", label = resourceReference(R.string.survey_demand_current_solution_exchange)),
            Option(key = "otc_desk", label = resourceReference(R.string.survey_demand_current_solution_otc_desk)),
            Option(key = "p2p", label = resourceReference(R.string.survey_demand_current_solution_p2p)),
            Option(
                key = "cash_exchange",
                label = resourceReference(R.string.survey_demand_current_solution_cash_exchange),
            ),
        ),
    ),
    FUNDS_OWNER(
        key = "funds_owner",
        title = resourceReference(R.string.survey_demand_funds_owner_question),
        hint = null,
        isOptional = true,
        options = listOf(
            Option(key = "own_only", label = resourceReference(R.string.survey_demand_funds_owner_own_only)),
            Option(key = "third_party", label = resourceReference(R.string.survey_demand_funds_owner_third_party)),
        ),
    ),
    BUSINESS_INTEREST(
        key = "business_interest",
        title = resourceReference(R.string.survey_demand_business_interest_question),
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