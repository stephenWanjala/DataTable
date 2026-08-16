# Theming

Every colour and text style is a parameter. The library has no Material dependency and no theme
of its own — it reads what you pass and nothing else.

## Colors

```kotlin
DataTable(
    // ...
    colors = DataTableDefaults.colors(
        container = Color.White,
        header = Color(0xFFF5F5F5),
        selectedRow = Color(0x4D1976D2),
        hoveredRow = Color(0x1A000000),
        rowAlternate = Color(0xFFFAFAFA),
        divider = Color(0xFFE0E0E0),
        checkboxChecked = Color(0xFF1976D2),
        focusedRowBorder = Color(0xFF1976D2),
    ),
)
```

| Token | Applies to |
|-------|-----------|
| `container` | Table background |
| `header` | Header row and footer background |
| `divider` | Row separators, freeze boundary, grouped-header underlines |
| `selectedRow` | Selected row background |
| `hoveredRow` | Hovered row background, and icon-button hover highlights |
| `rowAlternate` | Every other row. `Color.Transparent` (the default) disables striping |
| `expandedRow` | Background behind `expandContent` |
| `focusedRowBorder` | Leading edge marker on the keyboard-focused row |
| `checkboxChecked` / `checkboxUnchecked` / `checkboxCheckmark` | Selection checkboxes |
| `iconTint` | Sort arrows, expand chevrons, pagination arrows |
| `disabledContent` | Disabled pagination arrows |
| `onSurface` / `onSurfaceSecondary` | Reserved for text defaults |

## Text styles

```kotlin
DataTable(
    // ...
    textStyles = DataTableDefaults.textStyles(
        headerCell = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
        bodyCell = TextStyle(fontSize = 14.sp),
        footer = TextStyle(fontSize = 12.sp),
        pagination = TextStyle(fontSize = 14.sp),
        loading = TextStyle(fontSize = 14.sp),
        noData = TextStyle(fontSize = 16.sp),
    ),
)
```

Text colour lives on the `TextStyle`, not in `DataTableColors` — so a dark palette means setting
both.

## Density

```kotlin
DataTable(
    // ...
    density = DataTableDensity.COMPACT,   // DEFAULT, COMFORTABLE, or COMPACT
)
```

| Density | Vertical | Horizontal |
|---------|----------|-----------|
| `DEFAULT` | 16.dp | 16.dp |
| `COMFORTABLE` | 12.dp | 16.dp |
| `COMPACT` | 8.dp | 12.dp |

## A dark palette

There is no built-in dark preset. Both halves have to move together:

```kotlin
val darkColors = DataTableDefaults.colors(
    container = Color(0xFF1E1E1E),
    header = Color(0xFF2A2A2A),
    divider = Color(0xFF3A3A3A),
    selectedRow = Color(0x664F8CC9),
    hoveredRow = Color(0x14FFFFFF),
    rowAlternate = Color(0xFF242424),
    iconTint = Color(0xFFBBBBBB),
    disabledContent = Color(0xFF5A5A5A),
    checkboxChecked = Color(0xFF4F8CC9),
    checkboxUnchecked = Color(0xFF888888),
    focusedRowBorder = Color(0xFF4F8CC9),
)

val darkText = DataTableDefaults.textStyles(
    headerCell = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEDEDED)),
    bodyCell = TextStyle(fontSize = 14.sp, color = Color(0xFFDDDDDD)),
    footer = TextStyle(fontSize = 12.sp, color = Color(0xFFBBBBBB)),
    pagination = TextStyle(fontSize = 14.sp, color = Color(0xFFDDDDDD)),
)
```

Driving it from Material is just a matter of reading the scheme:

```kotlin
colors = DataTableDefaults.colors(
    container = MaterialTheme.colorScheme.surface,
    header = MaterialTheme.colorScheme.surfaceVariant,
    divider = MaterialTheme.colorScheme.outlineVariant,
    selectedRow = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
)
```

## Loading and empty states

```kotlin
DataTable(
    // ...
    loading = isLoading,
    loadingContent = { CircularProgressIndicator() },   // optional
    noDataContent = { Text("No results found") },       // optional
)
```

Both have plain built-in defaults. `loading = true` replaces the body entirely — there is no
overlay-while-showing-stale-rows mode.

The empty state shows whenever there are no rows to display, including an out-of-range page.

## Custom header and footer

```kotlin
DataTable(
    // ...
    hideDefaultHeader = true,
    headerContent = { /* your header composable */ },
    hideDefaultFooter = true,
    footerContent = { /* your footer composable */ },
)
```

Supplying `headerContent` replaces the built-in header entirely — including sorting, resizing, and
the select-all checkbox, which all live there. Reach for it when you want a toolbar above the
table rather than a replacement for the column headers.
