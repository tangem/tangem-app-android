package com.tangem.domain.jointaccount.safe

/**
 * Which Safe v1.5.0 master copy the proxy delegates to. The singleton is part of the CREATE2 deployment
 * data, so the counterfactual address depends on it: the same composition resolves to two different
 * addresses. Which one the backend deploys is an open alignment question (№21) — both stay supported
 * until it is answered, and the answer only decides which value the activation flow passes.
 */
enum class SafeSingleton(val address: String) {
    SAFE(address = "0xFf51A5898e281Db6DfC7855790607438dF2ca44b"),
    SAFE_L2(address = "0xEdd160fEBBD92E350D4D398fb636302fccd67C7e"),
}