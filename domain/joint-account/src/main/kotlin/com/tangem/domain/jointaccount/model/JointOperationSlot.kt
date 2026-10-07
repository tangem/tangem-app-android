package com.tangem.domain.jointaccount.model

/** Whether a joint account can start a new operation in a network. [UNKNOWN] means the backend could not be read. */
enum class JointOperationSlot {
    FREE,
    BUSY,
    UNKNOWN,
}