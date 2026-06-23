package com.cuentamorosos.model.validation

/**
 * Sanitizes user input by stripping Unicode control characters and normalizing whitespace.
 *
 * Strips characters in ranges:
 * - C0 controls: `\u0000-\u001F`
 * - DEL + C1 controls: `\u007F-\u009F`
 * - Zero-width and format chars: `\u200B-\u200F` (ZWSP, ZWNJ, ZWJ, LRM, RLM)
 * - Line/Paragraph separators: `\u2028-\u2029`
 * - RTL/LTR override + other bidi controls: `\u202A-\u202E`
 * - Byte Order Mark: `\uFEFF`
 *
 * After stripping control characters:
 * - Collapses multiple consecutive whitespace characters into a single space
 * - Trims leading and trailing whitespace
 *
 * Used by [EventValidator], [ProfileValidator], and [ItemValidator] before
 * domain validation to prevent Unicode spoofing attacks.
 */
fun sanitize(input: String): String {
    return input
        .replace(CONTROL_CHAR_REGEX, "")
        .replace(WHITESPACE_NORMALIZE_REGEX, " ")
        .replace(MULTI_SPACE_REGEX, " ")
        .trim()
}

/**
 * Matches Unicode control and format characters that should be stripped from user input.
 *
 * - `\u0000-\u0008`: C0 controls before TAB (NULL, SOH, ..., BS)
 * - `\u000B-\u000C`: Vertical tab, Form feed
 * - `\u000E-\u001F`: C0 controls after CR (SO, ..., US)
 * - `\u007F-\u009F`: DEL + C1 controls
 * - `\u200B-\u200F`: Zero-width space, ZWNJ, ZWJ, LRM, RLM
 * - `\u2028-\u202F`: Line sep, Paragraph sep, LRE, RLE, PDF, LRO, RLO
 * - `\uFEFF`: Byte Order Mark (BOM)
 *
 * TAB (`\u0009`), LF (`\u000A`), and CR (`\u000D`) are preserved and
 * normalized to spaces in the next step.
 */
private val CONTROL_CHAR_REGEX = Regex("[\u0000-\u0008\u000B-\u000C\u000E-\u001F\u007F-\u009F\u200B-\u200F\u2028-\u202F\uFEFF]")

/** Converts TAB, LF, and CR to spaces. */
private val WHITESPACE_NORMALIZE_REGEX = Regex("[\t\n\r]")

/** Collapses 2+ whitespace characters into a single space. */
private val MULTI_SPACE_REGEX = Regex("\\s{2,}")
