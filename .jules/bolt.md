## 2024-10-28 - Dynamic DB Sorting with un-keyed LazyColumn
**Learning:** Toggling favorite status triggers dynamic DB sorting. Combined with an un-keyed LazyColumn, this causes O(N) re-renders.
**Action:** Provide stable keys in Lazy lists.
## 2026-10-30 - Dynamic Tags Sorting with un-keyed LazyRow
**Learning:** Parsing tags from a string without distinct keys can cause duplicate key crashes and O(N) re-renders in LazyRow.
**Action:** Use .distinct() and stable keys in Lazy lists.
