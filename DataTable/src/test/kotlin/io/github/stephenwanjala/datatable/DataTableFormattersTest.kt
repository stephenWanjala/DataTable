package io.github.stephenwanjala.datatable

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The built-in formatters.
 *
 * Every case pins an explicit locale and zone: the default of the machine running the tests is
 * not something to assert against. What is worth asserting either way is the total behaviour —
 * a null, or a value of a type the formatter was not built for, must produce text rather than
 * an exception, because a formatter runs during layout where a throw takes the table with it.
 */
class DataTableFormattersTest {

    private val utc = ZoneId.of("UTC")

    @Test
    fun `number groups thousands and fixes the decimals`() {
        val format = DataTableFormatters.number(decimals = 2, locale = Locale.US)

        assertEquals("1,234.50", format(1234.5))
        assertEquals("1,234.57", format(1234.567))
        assertEquals("42.00", format(42))
    }

    @Test
    fun `number can drop the grouping`() {
        val format = DataTableFormatters.number(grouping = false, locale = Locale.US)

        assertEquals("100234", format(100234))
    }

    @Test
    fun `currency renders the symbol of its locale`() {
        val format = DataTableFormatters.currency(locale = Locale.US)

        assertEquals("$1,299.00", format(1299))
    }

    @Test
    fun `currency takes an explicit currency over the one the locale implies`() {
        val format = DataTableFormatters.currency(
            currency = Currency.getInstance("KES"),
            locale = Locale.US,
        )

        // The locale still decides the separators; only the currency has changed.
        assertEquals("KES1,299.00", format(1299))
    }

    @Test
    fun `currency honours a decimals override`() {
        val format = DataTableFormatters.currency(decimals = 0, locale = Locale.US)

        assertEquals("$1,299", format(1299.4))
    }

    @Test
    fun `percent reads a fraction by default`() {
        val format = DataTableFormatters.percent(decimals = 1, locale = Locale.US)

        assertEquals("12.3%", format(0.1234))
    }

    @Test
    fun `percent can read a value that is already a percentage`() {
        val format = DataTableFormatters.percent(fraction = false, locale = Locale.US)

        assertEquals("15%", format(15.0))
    }

    @Test
    fun `date formats java time values`() {
        val format = DataTableFormatters.date("dd MMM yyyy", locale = Locale.US, zone = utc)

        assertEquals("07 Mar 2026", format(LocalDate.of(2026, 3, 7)))
        assertEquals("07 Mar 2026", format(LocalDateTime.of(2026, 3, 7, 14, 30)))
    }

    @Test
    fun `date gives an instant a wall clock in the given zone`() {
        val instant = Instant.parse("2026-03-07T23:30:00Z")

        assertEquals(
            "2026-03-07 23:30",
            DataTableFormatters.date("yyyy-MM-dd HH:mm", Locale.US, utc)(instant),
        )
        assertEquals(
            "2026-03-08 02:30",
            DataTableFormatters.date("yyyy-MM-dd HH:mm", Locale.US, ZoneId.of("Africa/Nairobi"))(instant),
        )
    }

    @Test
    fun `date accepts a legacy Date and epoch milliseconds`() {
        val instant = Instant.parse("2026-03-07T09:15:00Z")
        val format = DataTableFormatters.date("yyyy-MM-dd HH:mm", Locale.US, utc)

        assertEquals("2026-03-07 09:15", format(Date.from(instant)))
        assertEquals("2026-03-07 09:15", format(instant.toEpochMilli()))
    }

    @Test
    fun `date falls back to toString for a pattern the value cannot satisfy`() {
        val format = DataTableFormatters.date("HH:mm", Locale.US, utc)

        // A LocalDate has no time of day; the alternative to this is throwing during layout.
        assertEquals("2026-03-07", format(LocalDate.of(2026, 3, 7)))
    }

    @Test
    fun `boolean renders words`() {
        val format = DataTableFormatters.boolean(trueText = "Active", falseText = "Inactive")

        assertEquals("Active", format(true))
        assertEquals("Inactive", format(false))
    }

    @Test
    fun `null renders as the null text, which is empty by default`() {
        assertEquals("", DataTableFormatters.number(locale = Locale.US)(null))
        assertEquals("", DataTableFormatters.currency(locale = Locale.US)(null))
        assertEquals("", DataTableFormatters.percent(locale = Locale.US)(null))
        assertEquals("", DataTableFormatters.date("yyyy", Locale.US, utc)(null))
        assertEquals("", DataTableFormatters.boolean()(null))

        assertEquals("—", DataTableFormatters.number(locale = Locale.US, nullText = "—")(null))
        assertEquals("n/a", DataTableFormatters.boolean(nullText = "n/a")(null))
    }

    @Test
    fun `a value of an unexpected type falls through to toString`() {
        assertEquals("n-a", DataTableFormatters.number(locale = Locale.US)("n-a"))
        assertEquals("n-a", DataTableFormatters.currency(locale = Locale.US)("n-a"))
        assertEquals("n-a", DataTableFormatters.percent(locale = Locale.US)("n-a"))
        assertEquals("today", DataTableFormatters.date("yyyy", Locale.US, utc)("today"))
        assertEquals("maybe", DataTableFormatters.boolean()("maybe"))
    }
}
