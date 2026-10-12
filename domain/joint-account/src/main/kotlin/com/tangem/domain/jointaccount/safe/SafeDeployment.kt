package com.tangem.domain.jointaccount.safe

/**
 * Whether the Safe already exists on the chain of an operation. Until the first operation in a network executes,
 * the Safe is only a CREATE2 address: nothing on the chain can be read about it, and services that inspect the
 * contract have nothing to inspect.
 */
enum class SafeDeployment {
    DEPLOYED,
    NOT_DEPLOYED,
}