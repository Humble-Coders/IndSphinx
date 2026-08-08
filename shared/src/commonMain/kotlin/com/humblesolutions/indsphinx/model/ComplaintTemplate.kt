package com.humblesolutions.indsphinx.model

data class ComplaintTemplate(
    val category: String = "",
    val problems: List<String> = emptyList(),
    /**
     * Admin-decided default priority, keyed by PROBLEM name — not by category.
     * "Making Noise" carries a priority; "AC" does not.
     *
     * Optional by design. Templates created before this feature have no map at
     * all, and those problems keep letting the occupant choose. Stored in
     * Firestore as a separate `problemPriorities` map so the `problems` array
     * older app builds read stays byte-for-byte unchanged.
     */
    val problemPriorities: Map<String, String> = emptyMap()
) {
    /**
     * The admin-decided priority for [problem], or `null` when the occupant is
     * free to pick it themselves.
     *
     * Returns null — meaning "occupant chooses" — for every uncertain case:
     * a blank problem, no entry, or a value this build does not recognise.
     * That last one matters: if a fifth priority level is ever added on the
     * admin side, this build falls back to the normal picker instead of
     * locking the form to a value it cannot render or submit.
     */
    fun priorityFor(problem: String): String? {
        if (problem.isBlank()) return null

        problemPriorities[problem]?.let { exact ->
            return canonicalPriority(exact)
        }
        // Tolerate casing / padding drift between the array entry and the key.
        val wanted = problem.trim()
        val loose = problemPriorities.entries.firstOrNull { (key, _) ->
            key.trim().equals(wanted, ignoreCase = true)
        }?.value
        return canonicalPriority(loose)
    }

    companion object {
        /** The only priorities this build can display and submit. */
        val PRIORITIES = listOf("Low", "Medium", "High", "Emergency")

        /** Maps a stored value onto its canonical casing, or null if unknown. */
        fun canonicalPriority(value: String?): String? {
            val v = value?.trim()
            if (v.isNullOrEmpty()) return null
            return PRIORITIES.firstOrNull { it.equals(v, ignoreCase = true) }
        }
    }
}
