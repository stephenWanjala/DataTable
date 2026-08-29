package io.github.stephenwanjala.datatable

import java.text.NumberFormat
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Currency
import java.util.Date
import java.util.Locale

/**
 * The text a column shows for an item. Cell rendering and clipboard copy both come through here,
 * so a copy carries what the cell reads; editing deliberately does not, opening on the raw value
 * instead.
 */
internal fun <T> DataTableHeader<T>.displayText(item: T): String {
    val raw = value?.invoke(item)
    return format?.invoke(raw) ?: raw?.toString() ?: ""
}

/**
 * Ready-made formatters for [DataTableHeader.format] — money, plain numbers, percentages, dates,
 * and booleans.
 *
 * ```kotlin
 * DataTableHeader<Order>(
 *     key = "total",
 *     title = "Total",
 *     value = { it.total },                       // raw, so the column sorts numerically
 *     format = DataTableFormatters.currency(),    // formatted, so the cell reads "$1,299.00"
 *     align = TextAlign.End,
 * )
 * ```
 *
 * Every formatter here is total: it renders `null` as its `nullText` and falls back to
 * `toString()` for a value of a type it cannot handle, rather than throwing. A formatter runs
 * during layout, once per visible cell, and an exception there would take the table down with it.
 *
 * Each returned formatter holds its locale and pattern, so build it once — in a `remember`, or
 * alongside the headers it belongs to — rather than per recomposition.
 */
object DataTableFormatters {

    /**
     * Formats a [Number] with a fixed number of decimal places.
     *
     * @param decimals Digits after the decimal separator. Values are rounded to fit, so `2` turns
     *                 `1234.5` into `1,234.50` and `1234.567` into `1,234.57`.
     * @param grouping Whether to group thousands (`1,234,567`). Turn it off for identifiers that
     *                 happen to be numbers, such as an order number.
     * @param locale Decides the decimal and grouping separators.
     * @param nullText What a null value renders as.
     */
    fun number(
        decimals: Int = 0,
        grouping: Boolean = true,
        locale: Locale = Locale.getDefault(),
        nullText: String = "",
    ): (Any?) -> String {
        val format = threadLocalFormat {
            NumberFormat.getNumberInstance(locale).also {
                it.minimumFractionDigits = decimals
                it.maximumFractionDigits = decimals
                it.isGroupingUsed = grouping
            }
        }
        return { value -> format.render(value, nullText) }
    }

    /**
     * Formats a [Number] as money, with the currency symbol and separators of a locale.
     *
     * @param currency Currency to render in, overriding the one [locale] implies. Use it when the
     *                 amounts are in a fixed currency but the separators should follow the user —
     *                 `Currency.getInstance("KES")` with a French locale gives `1 299,00 KES`.
     * @param decimals Digits after the decimal separator. Defaults to whatever the currency uses,
     *                 which is 2 for most and 0 for the likes of JPY.
     * @param locale Decides the symbol, the separators, and where the symbol sits.
     * @param nullText What a null value renders as.
     */
    fun currency(
        currency: Currency? = null,
        decimals: Int? = null,
        locale: Locale = Locale.getDefault(),
        nullText: String = "",
    ): (Any?) -> String {
        val format = threadLocalFormat {
            val instance = NumberFormat.getCurrencyInstance(locale)
            if (currency != null) instance.currency = currency
            if (decimals != null) {
                instance.minimumFractionDigits = decimals
                instance.maximumFractionDigits = decimals
            }
            instance
        }
        return { value -> format.render(value, nullText) }
    }

    /**
     * Formats a [Number] as a percentage.
     *
     * @param fraction Whether the stored value is a fraction. The default, `true`, reads `0.15`
     *                 as `15%`; pass `false` for a column that stores `15.0` and means the same.
     * @param decimals Digits after the decimal separator, of the percentage rather than of the
     *                 stored fraction: `1` renders `0.1234` as `12.3%`.
     * @param locale Decides the separators and where the percent sign sits.
     * @param nullText What a null value renders as.
     */
    fun percent(
        decimals: Int = 0,
        fraction: Boolean = true,
        locale: Locale = Locale.getDefault(),
        nullText: String = "",
    ): (Any?) -> String {
        val format = threadLocalFormat {
            NumberFormat.getPercentInstance(locale).also {
                it.minimumFractionDigits = decimals
                it.maximumFractionDigits = decimals
            }
        }
        return { value ->
            when (value) {
                null -> nullText
                // The percent instance multiplies by 100 itself, so a column already holding
                // whole percents is scaled back down before it gets there.
                is Number -> format.get().format(if (fraction) value else value.toDouble() / 100.0)
                else -> value.toString()
            }
        }
    }

    /**
     * Formats a date or time with a [DateTimeFormatter] pattern.
     *
     * Handles `java.time` values, a legacy [java.util.Date], and a [Long] read as epoch
     * milliseconds. A pattern the value cannot satisfy — `HH:mm` against a `LocalDate` — falls
     * back to the value's own `toString()` rather than throwing.
     *
     * @param pattern A [DateTimeFormatter] pattern, such as `dd MMM yyyy` or `yyyy-MM-dd HH:mm`.
     * @param locale Decides month and day names.
     * @param zone Zone used to give an instant a wall-clock date and time. Ignored by values that
     *             already carry their own date, such as `LocalDate`.
     * @param nullText What a null value renders as.
     */
    fun date(
        pattern: String,
        locale: Locale = Locale.getDefault(),
        zone: ZoneId = ZoneId.systemDefault(),
        nullText: String = "",
    ): (Any?) -> String {
        val formatter = DateTimeFormatter.ofPattern(pattern, locale).withZone(zone)
        return { value ->
            when (value) {
                null -> nullText
                is TemporalAccessor -> try {
                    formatter.format(value)
                } catch (_: DateTimeException) {
                    value.toString()
                }

                is Date -> formatter.format(value.toInstant())
                is Long -> formatter.format(Instant.ofEpochMilli(value))
                else -> value.toString()
            }
        }
    }

    /**
     * Renders a [Boolean] as words instead of `true` / `false`.
     *
     * @param trueText Text for `true`.
     * @param falseText Text for `false`.
     * @param nullText What a null value renders as — worth setting apart from [falseText] for a
     *                 nullable flag, where "not answered" and "no" are different answers.
     */
    fun boolean(
        trueText: String = "Yes",
        falseText: String = "No",
        nullText: String = "",
    ): (Any?) -> String = { value ->
        when (value) {
            null -> nullText
            is Boolean -> if (value) trueText else falseText
            else -> value.toString()
        }
    }
}

/**
 * A [NumberFormat] per thread.
 *
 * `NumberFormat` is not thread safe, and one formatter is shared by every cell in its column, so
 * handing out a single instance would be a data race the day composition stops running on one
 * thread. Building one per call instead would allocate on every cell of every frame.
 */
private fun threadLocalFormat(produce: () -> NumberFormat): ThreadLocal<NumberFormat> =
    ThreadLocal.withInitial(produce)

/** Formats a number, passing anything else through unchanged. */
private fun ThreadLocal<NumberFormat>.render(value: Any?, nullText: String): String = when (value) {
    null -> nullText
    is Number -> get().format(value)
    else -> value.toString()
}
