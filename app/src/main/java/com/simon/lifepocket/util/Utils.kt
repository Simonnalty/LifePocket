package com.simon.lifepocket.util

import java.time.LocalDate

/** 金额格式化：分 → 「18.00」 */
object Money {
    fun yuan(cents: Long): String {
        val abs = kotlin.math.abs(cents)
        val yuan = abs / 100
        val fen = (abs % 100).toString().padStart(2, '0')
        return "$yuan.$fen"
    }
}

/** CSV 生成（RFC 4180 的简化实现：逗号/引号/换行需要转义） */
object Csv {
    fun escape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value

    fun build(rows: List<List<String>>): String =
        rows.joinToString("\r\n") { row -> row.joinToString(",") { escape(it) } }
}

/** 年月与 epoch-day 区间（java.time 在 minSdk 26 起可用） */
object LpMonth {
    data class YMonth(val year: Int, val month: Int) {
        val label: String get() = "${month}月"
        fun firstDay(): LocalDate = LocalDate.of(year, month, 1)

        /** 本月 epochDay 闭区间 */
        fun epochRange(): LongRange {
            val start = firstDay().toEpochDay()
            val next = firstDay().plusMonths(1).toEpochDay()
            return start until next
        }

        fun prev(): YMonth = if (month == 1) YMonth(year - 1, 12) else YMonth(year, month - 1)
        fun next(): YMonth = if (month == 12) YMonth(year + 1, 1) else YMonth(year, month + 1)
    }

    fun now(): YMonth = LocalDate.now().let { YMonth(it.year, it.monthValue) }
}
