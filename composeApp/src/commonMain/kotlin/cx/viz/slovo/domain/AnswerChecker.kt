package cx.viz.slovo.domain

/**
 * Grades a typed free-recall answer (RU→EN) against a card's authored English string.
 * Pure: normalization + typo tolerance, no I/O. `/`-separated alternatives are each accepted.
 */
object AnswerChecker {
    enum class Verdict { CORRECT, ALMOST, WRONG }

    /** [canonical] is the expected string exactly as authored, for display. */
    data class Result(val verdict: Verdict, val canonical: String)

    fun check(typed: String, expected: String): Result {
        val t = normalize(typed)
        if (t.isEmpty()) return Result(Verdict.WRONG, expected)
        val candidates = expected.split('/').map { normalize(it) }.filter { it.isNotEmpty() }
        if (candidates.any { it == t }) return Result(Verdict.CORRECT, expected)
        val almost = candidates.any { editDistance(t, it) <= threshold(it.length) }
        return Result(if (almost) Verdict.ALMOST else Verdict.WRONG, expected)
    }

    private fun threshold(len: Int): Int = when {
        len <= 4 -> 0
        len <= 8 -> 1
        else -> 2
    }

    private fun normalize(s: String): String {
        val sb = StringBuilder()
        for (ch in s.lowercase()) {
            when {
                ch == '\'' || ch == '’' -> {}
                ch.isLetterOrDigit() -> sb.append(ch)
                else -> sb.append(' ')
            }
        }
        return sb.toString().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")
    }

    private fun editDistance(a: String, b: String): Int {
        val m = a.length; val n = b.length
        if (m == 0) return n
        if (n == 0) return m
        var prev = IntArray(n + 1) { it }
        var curr = IntArray(n + 1)
        for (i in 1..m) {
            curr[0] = i
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[n]
    }
}
