# Expense Tracker MVP Implementation Plan

## Goal

Build a Kotlin Multiplatform expense tracker for iOS and Android with fast expense entry, strong tag-based search, calendar summaries, category reports, multiple languages, and multiple currencies.

This plan intentionally excludes project setup, build configuration, and dependency selection.

## MVP Feature Scope

### Bottom Tabs

- Input
- Calendar
- Report
- Others

### Expense Input

- Add expense with amount, currency, category, note, tags, and date.
- Edit existing expense.
- Delete existing expense.
- Parse tags from note text using `#tag` syntax.
- Autocomplete existing tags while typing `#`.
- Save parsed tags with the expense.
- Keep entry fast with recent categories and recent tags.

### Categories

- Category has name and icon.
- Category can optionally have color and sort order.
- User can create, edit, archive, and reorder categories.
- Built-in starter categories are created on first launch.

### Tagging

- Tags are derived from notes and stored as first-class searchable values.
- Tag names are unique after normalization.
- Existing tags appear as autocomplete suggestions when the user types `#`.
- Transaction list supports filtering by one or more tags.
- Reports support filtering by one or more tags.
- Others screen includes tag usage stats.

### Calendar

- Monthly calendar view.
- Each day displays total expense amount.
- Tap a day to open transaction list for that date.
- Month header displays total spending for the selected month.
- Totals are shown in the user's base currency.

### Reports

- Monthly category pie chart.
- Yearly category pie chart.
- Tag filter for reports.
- Category breakdown list with amount and percentage.
- Month-over-month comparison column report.
- Reports display totals in the user's base currency.

### Languages

- English.
- Vietnamese.
- Chinese.
- App follows system language by default.
- User can override app language in Others.

### Currencies

- User selects base currency.
- Expense can be entered in any supported currency.
- Expense stores original amount and currency.
- Expense stores converted base amount for reports and calendar totals.
- Exchange rate can be manually provided or reused from the most recent known rate.

## Suggested Domain Model

### Expense

```kotlin
data class Expense(
    val id: ExpenseId,
    val originalAmount: MoneyAmount,
    val originalCurrencyCode: CurrencyCode,
    val baseAmount: MoneyAmount,
    val baseCurrencyCode: CurrencyCode,
    val exchangeRate: Decimal,
    val categoryId: CategoryId,
    val note: String,
    val tags: List<TagName>,
    val spentAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

### Category

```kotlin
data class Category(
    val id: CategoryId,
    val name: String,
    val icon: String,
    val color: String?,
    val sortOrder: Int,
    val archived: Boolean
)
```

### Tag

```kotlin
data class Tag(
    val name: TagName,
    val normalizedName: String,
    val createdAt: Instant,
    val lastUsedAt: Instant
)
```

### Currency Settings

```kotlin
data class CurrencySettings(
    val baseCurrencyCode: CurrencyCode,
    val availableCurrencyCodes: List<CurrencyCode>
)
```

### Exchange Rate

```kotlin
data class ExchangeRate(
    val fromCurrencyCode: CurrencyCode,
    val toCurrencyCode: CurrencyCode,
    val rate: Decimal,
    val effectiveDate: LocalDate
)
```

## Tag Behavior

### Tag Syntax

- A tag starts with `#`.
- Valid examples: `#work`, `#food`, `#trip2026`, `#ăn-trưa`, `#中文`.
- Tags can contain letters, numbers, underscores, hyphens, and supported Unicode letters.
- A tag ends at whitespace or punctuation that is not part of the allowed character set.
- Duplicate tags in the same note are stored once.

### Normalization

- Trim leading `#`.
- Trim surrounding whitespace.
- Compare case-insensitively where the platform locale allows stable behavior.
- Store the display form separately from the normalized lookup value if needed.
- Treat `#Work` and `#work` as the same tag.

### Parsing Examples

| Note | Parsed Tags |
| --- | --- |
| `Lunch #food #work` | `food`, `work` |
| `Coffee #work #work` | `work` |
| `Taxi #trip-2026` | `trip-2026` |
| `Ăn trưa #ăn-trưa` | `ăn-trưa` |
| `学习资料 #中文` | `中文` |

### Autocomplete

- Trigger suggestions when the cursor is inside a token that starts with `#`.
- Search existing tags by prefix after normalization.
- Show most-used matching tags first.
- If no matching tag exists, allow creating the typed tag by saving the note.
- Selecting a suggestion replaces only the active tag token.

### Tag Usage Stats

Show in Others:

- Tag name.
- Number of expenses using the tag.
- Total spending in base currency.
- Last used date.
- Optional trend: spending this month versus previous month.

Sort options:

- Most used.
- Highest spending.
- Recently used.
- Alphabetical.

## Screen Plans

### Input Tab

Primary purpose: add expenses quickly.

State:

- Amount input.
- Selected currency.
- Selected category.
- Note text.
- Active tag autocomplete query.
- Tag suggestions.
- Selected date/time.
- Exchange rate state.
- Save enabled or disabled.
- Validation errors.

Events:

- Amount changed.
- Currency selected.
- Category selected.
- Note changed.
- Tag suggestion selected.
- Date selected.
- Exchange rate changed.
- Save tapped.
- Reset form after successful save.

Behavior:

- Parse tags whenever note changes.
- Fetch autocomplete suggestions only when the cursor is in a `#` token.
- Save expense and upsert parsed tags.
- Convert original amount to base amount before saving.

### Calendar Tab

Primary purpose: find spending by day.

State:

- Selected month.
- Daily totals.
- Selected day.
- Transactions for selected day.
- Base currency.

Events:

- Previous month.
- Next month.
- Today.
- Day selected.
- Expense selected.

Behavior:

- Load daily totals for visible month.
- Display totals in base currency.
- Apply no tag filter by default.
- Open the transaction list for a selected day.

### Report Tab

Primary purpose: understand spending by category and tag.

State:

- Report period type: month or year.
- Selected month or year.
- Selected tag filters.
- Category pie chart data.
- Category breakdown rows.
- Month-over-month comparison rows.
- Total spending.
- Base currency.

Events:

- Period changed.
- Previous period.
- Next period.
- Tag filter changed.
- Category selected.
- Month-over-month row selected.

Behavior:

- Report queries always use base amount.
- Tag filters narrow all report numbers.
- Category pie chart and category list use the same filtered dataset.
- Month-over-month comparison shows current month, previous month, absolute change, and percentage change.

### Others Tab

Primary purpose: management and settings.

Sections:

- Categories.
- Tag usage stats.
- Currency settings.
- Language settings.
- Data management.

Behavior:

- Category edits should not rewrite historical expenses.
- Archiving a category hides it from input but preserves historical reports.
- Tag stats are derived from expenses.
- Language changes should update UI strings without altering user-created category or tag names.

## Transaction List Filtering

Transaction list should be reusable from:

- Calendar day detail.
- Report category detail.
- Global search or future history screen.

Filters:

- Date range.
- Category.
- One or more tags.
- Currency.
- Text search in note.

Tag filter behavior:

- Multiple selected tags should default to `AND` behavior for precise search.
- Consider adding `OR` later if users need broader discovery.
- Show active filters clearly.
- Allow removing a single tag filter without resetting the full query.

## Report Details

### Category Pie Chart

Input:

- Date range.
- Optional tag filters.
- Base currency.

Output per slice:

- Category id.
- Category name.
- Category icon.
- Category color.
- Total base amount.
- Percentage of filtered total.

Empty state:

- Show a quiet empty report if no expenses match the selected period and tag filters.

### Month-Over-Month Column Report

Purpose: show how spending changed compared with the previous month.

Columns:

- Category.
- Current month amount.
- Previous month amount.
- Change amount.
- Change percentage.

Rules:

- Use selected month as current month.
- Previous month is the immediately preceding calendar month.
- Apply selected tag filters to both months.
- Use base currency.
- If previous month is zero and current month is greater than zero, show `New`.
- If current month is zero and previous month is greater than zero, show `Stopped`.
- If both are zero, omit the row.

Sort options:

- Highest current month spending.
- Largest increase.
- Largest decrease.
- Category name.

## Shared Use Cases

### Expense Use Cases

- `AddExpenseUseCase`
- `UpdateExpenseUseCase`
- `DeleteExpenseUseCase`
- `GetExpensesUseCase`
- `GetTransactionsByDateUseCase`
- `GetTransactionsByFiltersUseCase`

### Tag Use Cases

- `ParseTagsFromNoteUseCase`
- `GetTagAutocompleteSuggestionsUseCase`
- `GetTagUsageStatsUseCase`
- `GetKnownTagsUseCase`

### Report Use Cases

- `GetDailyExpenseTotalsUseCase`
- `GetCategoryPieReportUseCase`
- `GetYearlyCategoryReportUseCase`
- `GetMonthOverMonthCategoryReportUseCase`

### Currency Use Cases

- `ConvertToBaseCurrencyUseCase`
- `GetCurrencySettingsUseCase`
- `UpdateBaseCurrencyUseCase`
- `GetExchangeRateUseCase`

## Persistence Plan

Tables or stored collections:

- Expenses.
- Categories.
- Tags.
- Expense tags join table if tags are normalized into their own table.
- Currency settings.
- Exchange rates.

Recommended indexing:

- Expense `spentAt`.
- Expense `categoryId`.
- Expense `baseCurrencyCode`.
- Tag `normalizedName`.
- Expense-tag pair.
- Exchange rate by source currency, target currency, and effective date.

Tag storage options:

- Store tags as a normalized join table for filtering and stats.
- Also keep original note text on expense.
- Avoid using note text parsing at query time.

## Localization Plan

Localize:

- Tab labels.
- Form labels.
- Validation messages.
- Empty states.
- Report labels.
- Settings labels.
- Built-in category display names before user customization.

Do not auto-translate:

- User-created category names.
- User-created tags.
- Expense notes.

Language behavior:

- Use system language by default.
- Store user override when selected.
- Fall back to English for missing strings.

## Currency Plan

MVP behavior:

- User selects a base currency.
- Each expense stores original amount and original currency.
- Each expense stores base amount at the time of entry.
- Manual exchange rate entry is available when expense currency differs from base currency.
- Reuse most recent exchange rate for the same currency pair when available.

Report behavior:

- Calendar and reports use base amount.
- Transaction detail shows both original amount and base amount when currencies differ.

## Testing Plan

### Unit Tests

Tag parser:

- Parses ASCII tags.
- Parses Vietnamese tags.
- Parses Chinese tags.
- Deduplicates repeated tags.
- Ignores incomplete `#`.
- Handles punctuation.
- Preserves stable display names.

Tag autocomplete:

- Returns prefix matches.
- Sorts by usage first.
- Handles case-insensitive matching.
- Replaces only active tag token.

Expense use cases:

- Saves original and base amount.
- Upserts tags on create.
- Replaces expense tags on update.
- Preserves historical category references.

Reports:

- Category pie totals match filtered expenses.
- Tag filters affect every report row.
- Month-over-month handles increase, decrease, new, stopped, and zero cases.
- Calendar daily totals group expenses by local date.

Currency:

- Converts original amount to base amount.
- Reuses latest exchange rate.
- Handles same-currency conversion as rate `1`.

### Integration Tests

- Add expense with note tags, then filter transaction list by tag.
- Add expenses across two months, then verify month-over-month report.
- Add expenses in multiple currencies, then verify calendar and report base totals.
- Archive category, then verify historical reports still show it.

### UI State Tests

- Input state shows tag suggestions while typing `#`.
- Selecting a tag suggestion updates note text.
- Report state refreshes when tag filter changes.
- Calendar state refreshes when month changes.

## Implementation Milestones

### Milestone 1: Domain Foundation

- Define expense, category, tag, currency, and report models.
- Implement tag parser.
- Implement currency conversion rules.
- Add repositories interfaces.
- Add unit tests for parser and conversion.

### Milestone 2: Persistence And Expense Entry

- Add persistence for expenses, categories, tags, currency settings, and exchange rates.
- Implement add, edit, delete expense use cases.
- Upsert tags from notes on expense save.
- Build Input tab state and events.
- Add tests for expense save and tag persistence.

### Milestone 3: Tag Autocomplete And Transaction Filtering

- Implement tag autocomplete suggestions.
- Add active tag-token detection in note input state.
- Implement reusable transaction list filters.
- Add transaction list tag filtering.
- Add tests for autocomplete and filter behavior.

### Milestone 4: Calendar

- Implement daily totals query.
- Build month calendar state and UI.
- Add selected day transaction list.
- Add tests for daily grouping and month navigation.

### Milestone 5: Reports

- Implement monthly category pie report.
- Implement yearly category pie report.
- Implement tag filters for reports.
- Implement month-over-month category column report.
- Add tests for filtered report calculations.

### Milestone 6: Others

- Build category management.
- Build tag usage stats.
- Build language settings.
- Build currency settings.
- Add tests for category archive behavior and tag stats.

### Milestone 7: Polish And Critical Flow Checks

- Verify add expense flow.
- Verify find expense by tag flow.
- Verify calendar day lookup flow.
- Verify report with tag filter flow.
- Verify language and currency setting flows.

## Critical User Flows

1. User adds an expense with note `Lunch #work #food`, then finds it later by filtering `#work`.
2. User types `#fo`, selects `#food` from autocomplete, and saves the expense.
3. User opens Calendar, taps a high-spending day, and reviews transactions.
4. User opens Report, filters by `#travel`, and sees category pie chart for the month.
5. User opens month-over-month report and sees which categories increased compared with last month.

## Open Product Decisions

- Should transaction tag filtering use `AND` only, or support `AND` and `OR` modes?
- Should tags be editable globally, including rename and merge?
- Should built-in categories differ by language or remain stable English-backed identifiers with localized display names?
- Should exchange rates be manual-only in MVP?
- Should income be excluded entirely from MVP or modeled now for future compatibility?
