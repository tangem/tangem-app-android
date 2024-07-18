package com.tangem.pagination.exception

class OperationWIthTheSameIdInProgress(operationId: String) : RuntimeException(
    "Operation is already in progress - id:$operationId ",
)
