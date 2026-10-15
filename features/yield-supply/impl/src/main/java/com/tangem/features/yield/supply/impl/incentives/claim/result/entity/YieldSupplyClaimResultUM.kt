package com.tangem.features.yield.supply.impl.incentives.claim.result.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.features.yield.supply.impl.R
import com.tangem.utils.StringsSigns.NEW_LINE
import com.tangem.utils.StringsSigns.WHITE_SPACE

@Immutable
internal data class YieldSupplyClaimResultUM(
    val status: Status,
    val total: TextReference,
    val serviceFee: TextReference?,
    val networkFee: TextReference?,
) {

    val title: TextReference
        get() = status.title(total)

    @Immutable
    internal sealed interface Status {

        val description: TextReference?

        fun title(total: TextReference): TextReference

        data object Submitted : Status {

            override val description: TextReference =
                resourceReference(R.string.yield_module_claim_in_progress_description)

            override fun title(total: TextReference): TextReference = combinedReference(
                resourceReference(R.string.yield_module_claim_in_progress_title),
                stringReference(NEW_LINE),
                total,
            )
        }

        /** @property timestamp epoch milliseconds */
        data class Confirmed(val timestamp: Long) : Status {

            override val description: TextReference
                get() = resourceReference(
                    id = R.string.send_date_format,
                    formatArgs = wrappedList(
                        timestamp.toTimeFormat(DateTimeFormatters.dateFormatter),
                        timestamp.toTimeFormat(),
                    ),
                )

            override fun title(total: TextReference): TextReference = combinedReference(
                resourceReference(R.string.yield_module_claim_success_toast),
                stringReference(WHITE_SPACE),
                total,
            )
        }

        data object Failed : Status {

            override val description: TextReference? = null

            override fun title(total: TextReference): TextReference =
                resourceReference(R.string.yield_module_claim_error_message)
        }
    }
}