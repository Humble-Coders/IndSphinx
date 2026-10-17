package com.humblesolutions.indsphinx.model

import kotlin.test.Test
import kotlin.test.assertEquals

class QuestionAnswerTest {

    private val max = QuestionAnswer.MAX_TEXT_LENGTH
    private fun text(n: Int) = "a".repeat(n)

    @Test
    fun capNeverSplitsAnEmoji() {
        val atBoundary = text(max - 1) + "😀"            // emoji would occupy units 499-500
        assertEquals(text(max - 1), QuestionAnswer.cap(atBoundary))
        assertEquals(text(max), QuestionAnswer.cap(text(max + 20)))
        assertEquals("short", QuestionAnswer.cap("short"))
    }

    @Test
    fun editsWithinTheLimitStand() {
        assertEquals("hello world", QuestionAnswer.limitEdit("hello", "hello world"))
        assertEquals(text(max), QuestionAnswer.limitEdit(text(max - 1), text(max)))
    }

    @Test
    fun typingIntoAFullAnswerIsRefusedInsteadOfDroppingItsEnd() {
        val full = "start " + text(max - 11) + " end."        // exactly 500 units
        assertEquals(max, full.length)
        val typedInMiddle = full.substring(0, 6) + "xyz" + full.substring(6)
        assertEquals(full, QuestionAnswer.limitEdit(full, typedInMiddle))
    }

    @Test
    fun deletingFromAFullAnswerStillWorks() {
        val full = text(max)
        assertEquals(text(max - 3), QuestionAnswer.limitEdit(full, text(max - 3)))
    }

    @Test
    fun aLongPasteKeepsItsStartAndEverythingAlreadyWritten() {
        val before = "Dear team, " + "end of my note."
        val paste = "Z".repeat(600)
        val proposed = "Dear team, " + paste + "end of my note."
        val result = QuestionAnswer.limitEdit(before, proposed)
        assertEquals(max, result.length)
        assertEquals(true, result.startsWith("Dear team, ZZZ"))
        assertEquals(true, result.endsWith("end of my note."))
    }

    @Test
    fun replacingEverythingWithALongPasteKeepsItsStart() {
        val result = QuestionAnswer.limitEdit("old answer", "N".repeat(700))
        assertEquals("N".repeat(max), result)
    }

    @Test
    fun aPasteNeverLeavesHalfAnEmoji() {
        val before = text(max - 1)                            // room for exactly one unit
        val result = QuestionAnswer.limitEdit(before, before + "😀😀")
        assertEquals(before, result)
        val roomForTwo = text(max - 2)
        assertEquals(roomForTwo + "😀", QuestionAnswer.limitEdit(roomForTwo, roomForTwo + "😀😀"))
    }
}
