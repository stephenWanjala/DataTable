# Grouping

`groupBy` maps each row to a group name. Rows are then rendered grouped, with optional header and
summary rows around each group.

```kotlin
DataTable(
    // ...
    groupBy = { it.department },
    groupHeaderContent = { groupName, rows ->
        Text("$groupName (${rows.size})", fontWeight = FontWeight.Bold)
    },
    groupSummaryContent = { groupName, rows ->
        Text("Average age: ${rows.map { it.age }.average().toInt()}")
    },
)
```

Both content slots are optional. Supplying neither groups the rows without any visual separator,
which is rarely what you want — `groupHeaderContent` is what makes grouping legible.

## Group headers

The header receives the group name and that group's rows, and spans the full table width:

```kotlin
groupHeaderContent = { department, rows ->
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Folder, contentDescription = null)
            Text("$department (${rows.size})", fontWeight = FontWeight.Bold)
        }
    }
}
```

## Summary rows

`groupSummaryContent` renders after the last row of each group — the natural place for subtotals:

```kotlin
groupSummaryContent = { _, rows ->
    Text("Total salary: $${"%,.0f".format(rows.sumOf { it.salary })}")
}
```

## Ordering

Groups appear in the order their first row appears in the sorted, paginated data. Sorting the
table therefore also reorders the groups.

Because grouping happens *after* sorting and pagination, a group can be split across pages — the
rows on page 2 form their own groups, headers and all. If you want whole groups per page, page by
group yourself with [`manualPagination`](sorting-and-paging.md#server-side-data).

## Interaction with the rest of the table

Keyboard navigation follows **display order**, so arrow keys walk through the grouped sequence and
skip over group headers and summaries rather than landing on them.

Alternating row colours and the focus indicator likewise use the display position.

## Limitations

!!! note "Groups are flat and always expanded"
    There is currently no collapse/expand for a group, no nesting of one group inside another, and
    no group-level selection. `groupBy` returns a single `String`, so grouping is one level deep.

    For hierarchical data — a chart of accounts, a bill of materials — you want a tree table, which
    the library does not have yet.
