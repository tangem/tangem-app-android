package com.tangem.domain.transaction.models

/**
 * How a Yield Mode gasless batch may pay its fee in the token it enters into or exits from the yield module.
 *
 * [Enter] — only the liquid balance pays, and part of it must remain to be entered.
 * [Exit] — the whole balance pays: the fee is taken after the batch has exited the module.
 */
enum class YieldModuleSameTokenRule { Enter, Exit }