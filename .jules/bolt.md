## 2024-10-28 - Dynamic DB Sorting with un-keyed LazyColumn
**Learning:** Toggling favorite status triggers dynamic DB sorting. Combined with an un-keyed LazyColumn, this causes O(N) re-renders.
**Action:** Provide stable keys in Lazy lists.
