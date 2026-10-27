package com.tangem.domain.jointaccount.model

/** A Joint Operation as the history keeps it: still in flight, or finished with its outcome */
sealed interface JointOperationRecord {

    val operationId: String

    data class InFlight(val operation: JointOperation) : JointOperationRecord {
        override val operationId: String get() = operation.operationId
    }

    data class Finished(val operation: FinishedJointOperation) : JointOperationRecord {
        override val operationId: String get() = operation.operationId
    }
}