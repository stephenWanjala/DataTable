# Selection & Expansion

Both are tracked by **row key** — the value returned by `itemKey` — not by the item itself.

That means selection and expansion survive item instances being replaced (a refresh from your
repository, say), and your row type never needs to implement `equals`/`hashCode`.

## Selection

```kotlin
var selectedKeys by remember { mutableStateOf<Set<Any>>(emptySet()) }

DataTable(
    items = people,
    headers = headers,
    itemKey = { it.id },                        // this is the selection identity
    showSelect = true,                          // show checkboxes
    selectionMode = SelectionMode.MULTI,        // NONE, SINGLE, or MULTI
    selectedKeys = selectedKeys,
    onSelectionChange = { selectedKeys = it },
)
```

### Modes

| Mode | Behaviour |
|------|-----------|
| `SelectionMode.NONE` | No selection UI. Row clicks still fire `onRowClick`. |
| `SelectionMode.SINGLE` | One row at a time. Clicking a selected row deselects it. |
| `SelectionMode.MULTI` | Multiple rows, with a select-all checkbox in the header. |

`selectionMode` defaults to `MULTI` when `showSelect = true`, and `NONE` otherwise.

### Resolving keys back to items

```kotlin
val selectedPeople = people.filter { it.id in selectedKeys }
```

If you need this often, keep a lookup rather than filtering repeatedly:

```kotlin
val byId = remember(people) { people.associateBy { it.id } }
val selectedPeople = selectedKeys.mapNotNull { byId[it] }
```

### Select-all

In `MULTI` mode the header checkbox selects every row in `items` — not just the current page.

!!! warning "Except under manual pagination"
    With [`manualPagination`](sorting-and-paging.md#server-side-data), `items` **is** the current
    page, so select-all covers only the loaded rows. The table cannot select rows it has never
    seen. Implement "select all N matching rows" yourself if you need it.

## Expansion

Same model, separate set:

```kotlin
var expandedKeys by remember { mutableStateOf<Set<Any>>(emptySet()) }

DataTable(
    // ...
    itemKey = { it.id },
    showExpand = true,
    expandedKeys = expandedKeys,
    onExpandChange = { expandedKeys = it },
    expandContent = { person ->
        Column(Modifier.padding(16.dp)) {
            Text("Details for ${person.name}")
            Text("Email: ${person.email}")
        }
    },
)
```

`showExpand` adds a chevron column at the leading edge. `expandContent` renders below the row when
it is expanded, spanning the full table width.

Expand-all and collapse-all are just set operations:

```kotlin
Button(onClick = { expandedKeys = people.map { it.id }.toSet() }) { Text("Expand All") }
Button(onClick = { expandedKeys = emptySet() }) { Text("Collapse All") }
```

## Why keys

The obvious alternative — `Set<Person>` — has three problems this design avoids:

1. It requires `equals`/`hashCode` on your row type, which is not always yours to change.
2. Selection breaks when items are replaced by equal-but-distinct instances, as happens on any
   refresh that rebuilds the list.
3. Checking "is everything selected" means comparing whole objects on every recomposition.

Keys are small, stable, and cheap to compare.
