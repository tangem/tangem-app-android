package com.tangem.features.jointaccount.blockaid

import com.tangem.domain.jointaccount.blockaid.JointOperationBlockaidGate.Verdict

enum class JointBlockaidSheetKind {
    RISKY,
    MALICIOUS,
    ;

    companion object {

        fun of(verdict: Verdict): JointBlockaidSheetKind? = when (verdict) {
            Verdict.Warning -> RISKY
            Verdict.Malicious -> MALICIOUS
            Verdict.Safe, is Verdict.NoVerdict -> null
        }
    }
}