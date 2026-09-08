package com.arvind.diaryai.util

object TranscriptProcessor {

    private val TYPE_KEYWORDS = mapOf(
        "idea" to "Idea", "ideas" to "Idea",
        "diary" to "Diary",
        "thought" to "Thoughts", "thoughts" to "Thoughts"
    )

    /**
     * Detects category from the first spoken word (e.g. "Idea, build an app that...")
     * and strips that leading keyword out of the transcript. Defaults to "Diary" if
     * no keyword is found, since that's the most common note type.
     */
    fun detectTypeAndClean(raw: String): Pair<String, String> {
        val trimmed = raw.trim()
        val firstWord = trimmed.substringBefore(' ').trim(',', '.', ':').lowercase()
        val type = TYPE_KEYWORDS[firstWord]
        return if (type != null) {
            val rest = trimmed.substringAfter(' ', "").trim()
            type to autoPunctuate(rest)
        } else {
            "Diary" to autoPunctuate(trimmed)
        }
    }

    /**
     * Very lightweight grammar cleanup for raw speech-to-text output:
     * - capitalizes sentence starts
     * - ensures terminal full stop
     * - collapses "comma"/"full stop"/"period"/"question mark" spoken tokens into punctuation
     * - trims double spaces
     * For production-quality punctuation restoration, swap this for a small on-device
     * seq2seq punctuation model; this rule-based pass is a reasonable v1.
     */
    fun autoPunctuate(text: String): String {
        if (text.isBlank()) return text
        var s = text
            .replace(Regex("\\bcomma\\b", RegexOption.IGNORE_CASE), ",")
            .replace(Regex("\\b(full stop|period)\\b", RegexOption.IGNORE_CASE), ".")
            .replace(Regex("\\bquestion mark\\b", RegexOption.IGNORE_CASE), "?")
            .replace(Regex("\\s+([,.?!])"), "$1")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

        // capitalize after sentence-ending punctuation and at the very start
        s = s.replaceFirstChar { it.uppercase() }
        s = Regex("([.!?]\\s+)([a-z])").replace(s) { m ->
            m.groupValues[1] + m.groupValues[2].uppercase()
        }
        if (s.isNotEmpty() && s.last() !in ".!?") s += "."
        return s
    }
}
