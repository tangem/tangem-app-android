package com.tangem.domain.apptheme.error

sealed class AppThemeModeError {

    data class DataError(val cause: Throwable) : AppThemeModeError()
}