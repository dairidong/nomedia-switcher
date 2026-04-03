# Simplified Chinese Localization Design

## Overview

This design localizes the Android app UI into Simplified Chinese while preserving English as the default fallback language.

The app name remains `NoMedia Switcher` in all locales. User-visible interface text, status labels, progress copy, and recoverable error messages should follow the device language when Simplified Chinese is active.

## Goals

- Add Simplified Chinese resources for user-visible app text.
- Keep English resources as the default fallback.
- Make Chinese and English output consistent across screens, progress UI, and surfaced task errors.
- Minimize architecture churn while removing user-facing hardcoded strings from Kotlin source.

## Non-Goals

- Translating the app name
- Adding Traditional Chinese or other locales
- Rewriting internal logging, database strings, or developer-only constants
- Introducing a full runtime i18n framework beyond Android resources

## Recommended Approach

Keep English strings in `res/values/strings.xml` and add a new `res/values-zh-rCN/strings.xml` for Simplified Chinese.

Move all user-visible hardcoded text in Compose screens and notification copy to string resources. For strings currently produced inside view models or use cases, stop returning raw display text from domain logic. Instead, return stable UI state or message keys that the UI layer maps to localized resources.

This is the best tradeoff because it follows Android's native localization model, preserves English fallback behavior, and avoids coupling domain code to a specific language.

## Alternatives Considered

### Option 1: Localize only the Compose layer

Pros:

- smallest immediate code diff

Cons:

- leaves surfaced failure and recovery messages in English
- creates mixed-language UI

### Option 2: Make Chinese the default resource and add English as a secondary locale

Pros:

- simple if Chinese is the only target audience

Cons:

- reverses Android's normal fallback shape
- makes future localization maintenance less clear

### Option 3: Add a custom string provider abstraction across the whole stack

Pros:

- fully centralizes text lookup

Cons:

- too much indirection for the current app size
- unnecessary churn for a focused localization pass

## Resource Strategy

### Default Resources

`res/values/strings.xml` remains the source of truth for default English strings, including notifications and all generic UI labels.

### Chinese Resources

Add `res/values-zh-rCN/strings.xml` with Simplified Chinese translations for the same keys, except `app_name`, which remains unchanged.

### Key Naming

Use descriptive keys grouped by UI surface, such as:

- album list titles and empty states
- settings labels and descriptions
- progress sheet copy
- toggle status labels
- permission and failure reasons

Avoid using raw English phrases as logic identifiers.

## UI Design

### Compose Screens

All visible Compose text should be loaded through `stringResource(...)` or equivalent resource APIs. This includes:

- top app bar titles
- buttons and action labels
- empty states
- helper copy
- status text shown inside album rows

### Progress And Notification Surfaces

The bottom sheet and foreground notifications should use localized strings through resource access, including formatted album names and action verbs.

### Runtime Language Behavior

The app should rely on Android's standard resource resolution:

- Chinese system locale uses `values-zh-rCN`
- all other locales fall back to default English resources

No in-app language picker is added in this iteration.

## Data And State Flow

### UI State

User-facing state should not carry final display strings when that text may need localization. Prefer one of these patterns:

- enum or sealed-state mapping to resource IDs in the UI layer
- lightweight presentation message keys resolved near the UI boundary

### Domain And Worker Errors

Business logic may still carry technical failure details internally, but text intended for users should be normalized before presentation so it can be localized consistently.

This applies to:

- directory grant guidance
- interrupted-task recovery messages
- SAF grant failure messages
- toggle failure reasons surfaced in the album list

## Testing Strategy

The implementation plan should cover:

- tests for any new UI-state-to-string mapping logic
- UI tests or unit tests verifying localized resource-backed labels are used instead of hardcoded literals where practical
- build verification that both default and `zh-rCN` resources compile cleanly

Manual verification should also confirm:

- English remains the default in non-Chinese locale
- Chinese text appears under Simplified Chinese system locale
- app name remains `NoMedia Switcher`

## Risks And Mitigations

### Risk: User-visible English strings remain hidden in view model logic

Mitigation:

- explicitly scan current Kotlin sources for hardcoded visible text
- move surfaced messages to resource-backed mapping

### Risk: Over-localizing internal constants breaks behavior

Mitigation:

- only translate user-visible strings
- leave intent actions, database names, MIME types, and storage constants unchanged

### Risk: String formatting becomes inconsistent across UI and notifications

Mitigation:

- centralize shared labels in string resources
- use formatted resource strings for album-specific messages

## Acceptance Criteria

- The app follows the system language for Simplified Chinese versus default English.
- The app name remains `NoMedia Switcher` in Chinese systems.
- Main screens, settings, progress sheet, and notifications no longer show hardcoded English user-visible text.
- User-visible status and failure messages are localized consistently.
- Default English behavior continues to work when Chinese resources are not selected.
