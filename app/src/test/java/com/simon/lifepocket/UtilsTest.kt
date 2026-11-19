package com.simon.lifepocket

import com.simon.lifepocket.util.Csv
import com.simon.lifepocket.util.LpMonth
import com.simon.lifepocket.util.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsTest {

    // ---- 金额格式化 ----
    @Test
    fun money_formatsCentsToYuan() {
        assertEquals("18.00", Money.yuan(1800))
        assertEquals("0.00", Money.yuan(0))
        assertEquals("9.90", Money.yuan(990))
        assertEquals("3284.60", Money.yuan(328460))
        assertEquals("1286.05", Money.yuan(128605))
    }

    @Test
    fun money_handlesNegativeSafely() {
        // 结余可能为负：显示取绝对值，符号由调用方决定
        assertEquals("36.50", Money.yuan(-3650))
    }

    // ---- CSV 转义 ----
    @Test
    fun csv_escapePlain() {
        assertEquals("午饭", Csv.escape("午饭"))
    }

    @Test
    fun csv_escapeCommaAndQuote() {
        assertEquals("\"奶茶,三分糖\"", Csv.escape("奶茶,三分糖"))
        assertEquals("\"说了\"\"好的\"\"\"", Csv.escape("说了\"好的\""))
        assertEquals("\"多行\n备注\"", Csv.escape("多行\n备注"))
    }

    @Test
    fun csv_buildJoinsRows() {
        val csv = Csv.build(
            listOf(
                listOf("日期", "类型", "金额(元)"),
                listOf("2026-11-19", "支出", "18.00"),
            )
        )
        assertEquals("日期,类型,金额(元)\r\n2026-11-19,支出,18.00", csv)
    }

    // ---- 月份区间 ----
    private fun rangeDays(m: LpMonth.YMonth): Long = m.epochRange().last - m.epochRange().first + 1

    @Test
    fun month_epochRangeLengths() {
        assertEquals(30L, rangeDays(LpMonth.YMonth(2026, 11)))  // 小月 30 天
        assertEquals(31L, rangeDays(LpMonth.YMonth(2026, 12)))  // 大月 31 天
        assertEquals(28L, rangeDays(LpMonth.YMonth(2026, 2)))   // 平年二月
        assertEquals(29L, rangeDays(LpMonth.YMonth(2024, 2)))   // 闰年二月
    }

    @Test
    fun month_prevNextWrapYears() {
        assertEquals(LpMonth.YMonth(2025, 12), LpMonth.YMonth(2026, 1).prev())
        assertEquals(LpMonth.YMonth(2027, 1), LpMonth.YMonth(2026, 12).next())
        assertEquals(LpMonth.YMonth(2026, 10), LpMonth.YMonth(2026, 11).prev())
    }

    @Test
    fun month_label() {
        assertEquals("11月", LpMonth.YMonth(2026, 11).label)
    }
}
