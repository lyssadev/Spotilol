package com.project.lol.searchEngine

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.pow

private val WORD_REGEX = Regex("[a-zA-Z0-9]+")
private val OR_REGEX = Regex("\\s+or\\s+")

private fun extractKeywords(raw: String): List<String> = WORD_REGEX.findAll(raw).map { it.value }.toList()

private fun normalize(text: String?): String = text?.lowercase(Locale.ROOT) ?: ""

private fun computePriority(q: String, text: String): Int = when {
    text == q -> 4
    text.startsWith(q) -> 3
    text.contains(" $q") -> 2
    text.contains(q) -> 1
    else -> 0
}

private fun countOverlap(s1: String, s2: String): Int {
    val counts = IntArray(26)
    for (c in s1) {
        if (c in 'a'..'z') counts[c - 'a']++
    }
    var shared = 0
    for (c in s2) {
        if (c in 'a'..'z') {
            val idx = c - 'a'
            if (counts[idx] > 0) {
                shared++
                counts[idx]--
            }
        }
    }
    return shared
}

class GenericSearchEngine<T>(
    private val maxResult: Int,
    private val scoreCutoff: Double = 0.4,
    private val minOverlapRatio: Double = 0.6,
    private val usePartial: Boolean = true
) {

    data class SearchResult<T>(val item: T, val score: Double)

    private data class ScoredItem<T>(
        val item: T,
        val score: Double,
        val priority: Int,
        val normText: String,
        val originalIndex: Int
    )

    private data class FieldMatch(
        val score: Double,
        val priority: Int,
        val normText: String
    )

    private data class QueryInfo(
        val isAndLogic: Boolean,
        val keywords: List<String>
    ) {
        companion object {
            fun parse(raw: String): QueryInfo {
                val hasOr = OR_REGEX.containsMatchIn(raw)
                val cleaned = if (hasOr) OR_REGEX.replace(raw, " ") else raw
                return QueryInfo(isAndLogic = !hasOr, keywords = extractKeywords(cleaned))
            }
        }
    }

    fun filterWithScores(
        data: List<T>,
        query: String,
        extractor: SearchableFieldExtractor<T>
    ): List<SearchResult<T>> {
        if (data.isEmpty() || maxResult <= 0) return emptyList()

        val qNorm = normalize(query)
        if (qNorm.isEmpty()) return emptyList()

        val qi = QueryInfo.parse(qNorm)
        if (qi.keywords.isEmpty()) return emptyList()

        val results = ArrayList<ScoredItem<T>>()

        data.forEachIndexed { index, item ->
            val fields = extractor.getSearchableFields(item)
            if (fields.isNotEmpty()) {
                val normFields = fields.map { normalize(it) }

                val scored = if (qi.isAndLogic) {
                    scoreAnd(qi.keywords, normFields, item, index)
                } else {
                    scoreOr(qi.keywords, normFields, item, index)
                }

                if (scored != null) results.add(scored)
            }
        }

        results.sortWith(COMPARATOR)
        return limitResults(results)
    }

    fun filter(
        data: List<T>,
        query: String,
        extractor: SearchableFieldExtractor<T>
    ): List<T> = filterWithScores(data, query, extractor).map { it.item }

    private fun scoreAnd(
        keywords: List<String>,
        normFields: List<String>,
        item: T,
        idx: Int
    ): ScoredItem<T>? {
        var totalScore = 0.0
        var minScore = Double.MAX_VALUE
        var bestPriority = 0
        var bestText = ""

        for (kw in keywords) {
            val best = bestFieldMatch(kw, normFields) ?: return null

            totalScore += best.score
            minScore = minOf(minScore, best.score)
            if (best.priority > bestPriority) {
                bestPriority = best.priority
                bestText = best.normText
            }
        }

        val finalScore = (totalScore / keywords.size) + (minScore * 0.5)
        return ScoredItem(item, finalScore, bestPriority, bestText, idx)
    }

    private fun scoreOr(
        keywords: List<String>,
        normFields: List<String>,
        item: T,
        idx: Int
    ): ScoredItem<T>? {
        var maxScore = 0.0
        var matchCount = 0
        var bestPriority = 0
        var bestText = ""

        for (kw in keywords) {
            val fm = bestFieldMatch(kw, normFields)
            if (fm != null) {
                matchCount++
                if (fm.score > maxScore) {
                    maxScore = fm.score
                    bestPriority = fm.priority
                    bestText = fm.normText
                }
            }
        }

        if (matchCount == 0) return null

        val finalScore = maxScore + matchCount * 0.2
        return ScoredItem(item, finalScore, bestPriority, bestText, idx)
    }

    private fun bestFieldMatch(kw: String, normFields: List<String>): FieldMatch? {
        var best: FieldMatch? = null

        for (text in normFields) {
            if (text.isEmpty()) continue

            val score = computeScore(kw, text)
            if (score < scoreCutoff) continue

            val priority = computePriority(kw, text)

            if (best == null || score > best.score ||
                (score == best.score && priority > best.priority)
            ) {
                best = FieldMatch(score, priority, text)
            }
        }
        return best
    }

    private fun computeScore(q: String, text: String): Double {
        val lenQ = q.length
        val lenT = text.length
        val lenSum = lenQ + lenT
        if (lenSum == 0) return 1.0

        if (usePartial && lenQ <= 3 && !text.contains(q)) return 0.0

        if (lenQ > 1 && minOverlapRatio > 0) {
            val overlap = countOverlap(q, text)
            var effectiveRatio = minOverlapRatio
            if (lenQ > 5) {
                effectiveRatio = maxOf(0.3, minOverlapRatio * (5.0 / lenQ))
            }
            val minRequired = ceil(lenQ * effectiveRatio).toInt()
            if (overlap < minRequired) return 0.0
        }

        val dist = levenshteinDistance(q, text)
        val fullScore = (lenSum - dist).toDouble() / lenSum

        if (usePartial && lenQ != lenT) {
            val partialScore = partialSimilarity(q, text) * computeRelevance(q, text)
            return maxOf(fullScore, partialScore)
        }

        return fullScore
    }

    private fun computeRelevance(q: String, text: String): Double {
        val lenQ = q.length
        val lenT = text.length

        val coverage = lenQ.toDouble() / lenT
        val coverageFactor = coverage.pow(0.25)

        val matchIndex = text.indexOf(q)

        if (matchIndex == 0) return coverageFactor

        if (matchIndex > 0) {
            if (text[matchIndex - 1] == ' ') return coverageFactor * 0.85

            val relativePos = matchIndex.toDouble() / lenT
            val positionFactor = maxOf(0.3, 0.7 - 0.4 * relativePos)
            return coverageFactor * positionFactor
        }

        val bestPos = text.indexOf(q[0])

        if (bestPos < 0) return coverageFactor * 0.3

        val positionFactor = when {
            bestPos == 0 -> 0.9
            text[bestPos - 1] == ' ' -> 0.75
            else -> maxOf(0.25, 0.6 - 0.35 * (bestPos.toDouble() / lenT))
        }

        return coverageFactor * positionFactor * 0.9
    }

    private fun partialSimilarity(q: String, text: String): Double {
        val (shortStr, longStr) = if (q.length <= text.length) q to text else text to q

        val lenS = shortStr.length
        val lenL = longStr.length

        if (lenS == 0) return if (lenL == 0) 1.0 else 0.0

        var bestScore = 0.0
        val minWindow = maxOf(1, lenS - 1)
        val maxWindow = minOf(lenL, lenS + 2)

        for (windowSize in minWindow..maxWindow) {
            for (start in 0..lenL - windowSize) {
                val substr = longStr.substring(start, start + windowSize)
                val dist = levenshteinDistance(shortStr, substr)
                val lenSum = lenS + windowSize
                val score = (lenSum - dist).toDouble() / lenSum

                if (score > bestScore) bestScore = score
                if (bestScore >= 1.0) return 1.0
            }
        }

        return bestScore
    }

    private fun limitResults(items: List<ScoredItem<T>>): List<SearchResult<T>> {
        val size = minOf(maxResult, items.size)
        return items.take(size).map { SearchResult(it.item, it.score) }
    }

    companion object {

        private val COMPARATOR: Comparator<ScoredItem<*>> =
            compareByDescending<ScoredItem<*>> { it.priority }
                .thenByDescending { it.score }
                .thenBy { it.normText }

        fun levenshteinDistance(s1: String, s2: String): Int {
            val len1 = s1.length
            val len2 = s2.length
            var prev = IntArray(len2 + 1)
            var curr = IntArray(len2 + 1)

            for (j in 0..len2) prev[j] = j

            for (i in 1..len1) {
                curr[0] = i
                val c1 = s1[i - 1]
                for (j in 1..len2) {
                    val cost = if (c1 == s2[j - 1]) 0 else 1
                    curr[j] = minOf(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
                }
                val tmp = prev
                prev = curr
                curr = tmp
            }
            return prev[len2]
        }
    }
}