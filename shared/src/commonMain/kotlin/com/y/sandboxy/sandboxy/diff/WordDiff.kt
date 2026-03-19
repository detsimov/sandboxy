package com.y.sandboxy.sandboxy.diff

enum class DiffType { Unchanged, Added, Removed }

data class DiffSegment(val text: String, val type: DiffType)

object WordDiff {
    fun diff(textA: String, textB: String): List<DiffSegment> {
        val wordsA = textA.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val wordsB = textB.split(Regex("\\s+")).filter { it.isNotEmpty() }

        val lcs = lcs(wordsA, wordsB)
        val result = mutableListOf<DiffSegment>()

        var ia = 0
        var ib = 0
        var il = 0

        while (il < lcs.size) {
            // Emit removed words from A before next LCS word
            while (ia < wordsA.size && wordsA[ia] != lcs[il]) {
                appendSegment(result, wordsA[ia], DiffType.Removed)
                ia++
            }
            // Emit added words from B before next LCS word
            while (ib < wordsB.size && wordsB[ib] != lcs[il]) {
                appendSegment(result, wordsB[ib], DiffType.Added)
                ib++
            }
            // Emit common word
            appendSegment(result, lcs[il], DiffType.Unchanged)
            ia++
            ib++
            il++
        }

        // Remaining words
        while (ia < wordsA.size) {
            appendSegment(result, wordsA[ia], DiffType.Removed)
            ia++
        }
        while (ib < wordsB.size) {
            appendSegment(result, wordsB[ib], DiffType.Added)
            ib++
        }

        return result
    }

    private fun appendSegment(result: MutableList<DiffSegment>, word: String, type: DiffType) {
        val last = result.lastOrNull()
        if (last != null && last.type == type) {
            result[result.lastIndex] = last.copy(text = "${last.text} $word")
        } else {
            result.add(DiffSegment(word, type))
        }
    }

    private fun lcs(a: List<String>, b: List<String>): List<String> {
        val m = a.size
        val n = b.size
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1] + 1
                } else {
                    maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }

        // Backtrack
        val result = mutableListOf<String>()
        var i = m
        var j = n
        while (i > 0 && j > 0) {
            when {
                a[i - 1] == b[j - 1] -> {
                    result.add(a[i - 1])
                    i--
                    j--
                }
                dp[i - 1][j] > dp[i][j - 1] -> i--
                else -> j--
            }
        }
        return result.reversed()
    }
}
