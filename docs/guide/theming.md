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
| `focusedCellBorder` | Outline around the focused cell, once `cellNavigation` is on |
| `selectedCell` | Wash over cells in a selected range. Translucent, and the cursor cell is left unwashed |
| `editingCell` | Background behind an open cell editor |
| `invalidCellBorder` | Replaces `focusedCellBorder` while an editor's value has been rejected |
| `draggedColumn` | Wash over the header being dragged, once `reorderableColumns` is on |
| `columnDropIndicator` | The line marking the edge a dragged column will land against |
| `checkboxChecked` / `checkboxUnchecked` / `checkboxCheckmark` | Selection checkboxes |
| `iconTint` | Sort arrows, expand chevrons, pagination arrows |
| `disabledContent` | Disabled pagination arrows |
| `onSurface` | The caret in an open cell editor. Also available to custom cell content |
| `onSurfaceSecondary` | Muted content colour, for custom cell content |

`focusedCellBorder`, `selectedCell`, `editingCell`, and `invalidCellBorder` only ever draw under
[cell navigation or editing](editing.md) — a table that opts into neither can ignore them.

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
        cellEditor = TextStyle(fontSize = 14.sp),
    ),
)
```

Text colour lives on the `TextStyle`, not in `DataTableColors` — so a dark palette means setting
both.

`cellEditor` styles the text inside an open cell editor. It matches `bodyCell` by default, which
is what stops a cell jumping as it goes into edit mode — if you restyle one, restyle the other.

## Density

```kotlin
DataTable(
    // ...
    density = DataTableDensity.COMPACT,   // DEFAULT, COMFORTABLE, or COMPACT
)
```

| Density | Vertical padding | Horizontal padding | Default row height |
|---------|------------------|--------------------|--------------------|
| `DEFAULT` | 16.dp | 16.dp | 52.dp |
| `COMFORTABLE` | 12.dp | 16.dp | 44.dp |
| `COMPACT` | 8.dp | 12.dp | 36.dp |

Density sets the padding, and the default row height follows from it:
`DataTableDefaults.rowHeight(density)` is that padding either side of one line of body text. Pass
`DataTable` a `rowHeight` of your own for cells that need more room, or `rowHeight = null` for
rows that size to their content — see
[Row height](columns.md#row-height-and-horizontal-virtualization).

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
    focusedCellBorder = Color(0xFF4F8CC9),
    selectedCell = Color(0x334F8CC9),
    editingCell = Color(0xFF2F2F2F),
    onSurface = Color(0xFFEDEDED),
)

val darkText = DataTableDefaults.textStyles(
    headerCell = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEDEDED)),
    bodyCell = TextStyle(fontSize = 14.sp, color = Color(0xFFDDDDDD)),
    footer = TextStyle(fontSize = 12.sp, color = Color(0xFFBBBBBB)),
    pagination = TextStyle(fontSize = 14.sp, color = Color(0xFFDDDDDD)),
    cellEditor = TextStyle(fontSize = 14.sp, color = Color(0xFFDDDDDD)),
)
```

!!! warning "`editingCell` is white until you move it"
    It defaults to `Color(0xFFFFFFFF)` — the one token that will flash a white box in an otherwise
    dark table the first time someone opens an editor. `onSurface` paints the caret in that editor,
    so it has to move with it or the cursor is invisible.

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
