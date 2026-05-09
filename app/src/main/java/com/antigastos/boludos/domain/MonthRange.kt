package com.antigastos.boludos.domain

import java.time.YearMonth
import java.time.ZoneId

fun YearMonth.toPeriodString(): String =
    String.format("%04d-%02d", year, monthValue)

fun YearMonth.startEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

fun YearMonth.endExclusiveEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
