package com.tangem.core.ui.components.audits

import com.tangem.core.ui.extensions.TextReference

/**
 * Audit label component UI model
 *
 * @property text text reference
 * @property type type of label
 *
 * @author Andrew Khokhlov on 01/10/2024
 */
data class AuditLabelUM(val text: TextReference, val type: Type) {

    enum class Type {

        /** Red. Like, "risky" */
        Prohibition,

        /** Yellow. Like, "caution" */
        Warning,

        /** Blue. Like, "trusted" */
        Permit,
    }
}
