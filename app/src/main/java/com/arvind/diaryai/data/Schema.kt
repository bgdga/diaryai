package com.arvind.diaryai.data

/**
 * Column schemas mirror the user's original Google Forms exactly, so historical
 * exported data (Google Sheets -> TSV) can be dropped in without transformation.
 */
object Schema {

    // ---------- Voice diary / idea / thought notes ----------
    // File: notes.tsv  (append-only log; one row per voice note)
    // Transcribed by Android's built-in Google voice input (on-device when offline
    // recognition is downloaded on the phone) in whichever language you pick before recording.
    val NOTES_HEADER = listOf(
        "date", "time", "type",   // type = Diary | Idea | Thoughts (from first spoken word)
        "transcript", "language"  // language = English | Hindi, as selected before recording
    )
    val NOTE_LANGUAGES = listOf("English" to "en-IN", "Hindi" to "hi-IN")

    // ---------- Expenses ----------
    // File: expenses.tsv — mirrors "InEx (-out, +In)" form fields 1:1
    val EXPENSE_HEADER = listOf(
        "date", "time",
        "amount",        // signed: -n = expense, +n / n = income
        "cash_online",   // Cash | Online
        "whose_exp",     // Arv | Ntn
        "category",      // one of EXPENSE_CATEGORIES
        "comments"
    )

    val EXPENSE_CASH_ONLINE = listOf("Cash", "Online")
    val EXPENSE_WHOSE = listOf("Arv", "Ntn")
    val EXPENSE_CATEGORIES = listOf(
        "Restaurant", "Grocery", "Travel", "House", "Body", "Clothes",
        "Entertainment", "Business", "Education", "Other",
        "Money IN - Job Earning", "Money IN - Business Earning",
        "Money IN/OUT - Home (Chhotu, Mom, Sush)", "Money IN/OUT - NTN",
        "Money IN/OUT - Lent/Borrowed (- given, + taken)", "Cash-UPI/self-transfer"
    )

    // ---------- Daily diary metrics (one row per day) ----------
    // File: diary_metrics.tsv — mirrors "Diary" form fields 1:13 (excluding free-text
    // Headlines/Thoughts/Ideas, which live in notes.tsv keyed by the same date)
    val DIARY_METRICS_HEADER = listOf(
        "diary_date",
        "family_social",
        "exam_prep_study",
        "book_writing_business_prep",
        "work_to_earn_today",
        "daily_target",
        "target_achieved",      // 1-5 stars
        "route_location",       // comma-joined multi-select: Room|Boys class|Girls class|Canteen|Market nearby|Other:<text>
        "work_intense",         // 1-5, 1=Least .. 5=Brain blasting Workload
        "mood"                  // -5..5
    )
    val ROUTE_OPTIONS = listOf("Room", "Boys class", "Girls class", "Canteen", "Market nearby")
    const val MOOD_MIN = -5
    const val MOOD_MAX = 5

    // Filenames, all under context.getExternalFilesDir(null)/DiaryAI/
    const val DIR_NAME = "DiaryAI"
    const val DIR_AUDIO = "audio"
    const val FILE_NOTES = "notes.tsv"
    const val FILE_EXPENSES = "expenses.tsv"
    const val FILE_DIARY_METRICS = "diary_metrics.tsv"
}
