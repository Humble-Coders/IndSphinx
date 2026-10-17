package com.humblesolutions.indsphinx.model

/**
 * Limits on a resident's answer to a notice-board or notification question.
 *
 * Shared by both kinds of question so the two answer screens behave the same.
 * Matches the 500-character limit the admin portal puts on the question itself.
 *
 * "Characters" means UTF-16 units, which is what Kotlin's `length` and the
 * portal's JavaScript both count, so the limit reads the same everywhere. The
 * iOS app counts the same way rather than by visible characters.
 *
 * The Firestore rules carry a looser ceiling of their own. It is deliberately
 * higher, so a resident still on an app version without this limit is not
 * turned away for writing a long answer.
 */
object QuestionAnswer {
    const val MAX_TEXT_LENGTH = 500

    /**
     * [text] cut to the limit, without leaving the first half of a surrogate
     * pair at the end. A plain `take()` can split an emoji, and the stray half
     * is replaced with "?" when the answer is saved.
     */
    fun cap(text: String): String = capTo(text, MAX_TEXT_LENGTH)

    /**
     * What a text field should hold after an edit turned [previous] into
     * [proposed]. Within the limit the edit simply stands. Past it:
     *  - an answer already at the limit stays as it was, so typing in the
     *    middle of a full answer can never silently push its end off;
     *  - otherwise the text before and after the edit is kept, and only as
     *    much of the inserted text as fits, so a long paste keeps its start
     *    and nothing already written is lost.
     */
    fun limitEdit(previous: String, proposed: String): String {
        if (proposed.length <= MAX_TEXT_LENGTH) return proposed
        if (previous.length >= MAX_TEXT_LENGTH) return previous
        // commonPrefixWith / commonSuffixWith never end inside a surrogate pair.
        val prefix = previous.commonPrefixWith(proposed).length
        val suffix = previous.substring(prefix).commonSuffixWith(proposed.substring(prefix)).length
        val inserted = proposed.substring(prefix, proposed.length - suffix)
        // prefix + suffix is part of previous, which is under the limit, so
        // there is always room for at least one unit.
        val room = MAX_TEXT_LENGTH - prefix - suffix
        val kept = capTo(inserted, room)
        // Nothing of the insertion fits (one unit left, an emoji pasted):
        // leave the answer exactly as it was, as iOS does.
        if (kept.isEmpty()) return previous
        return proposed.substring(0, prefix) + kept + proposed.substring(proposed.length - suffix)
    }

    private fun capTo(text: String, max: Int): String {
        if (text.length <= max) return text
        if (max <= 0) return ""
        val end = if (text[max - 1].isHighSurrogate()) max - 1 else max
        return text.substring(0, end)
    }
}
