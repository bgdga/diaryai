# Diary AI — offline personal diary + expense tracker (Android)

A real, importable Android Studio project matching your two Google Forms exactly — see
`app/src/main/java/com/arvind/diaryai/data/Schema.kt` for the exact field mapping.

All data (`notes.tsv`, `expenses.tsv`, `diary_metrics.tsv`) is stored **locally only** as
tab-separated files under `Android/data/com.arvind.diaryai/files/DiaryAI/` on the phone —
open them directly in Excel/Sheets by copying off the device. Nothing leaves the phone.

## How this now gets you a real, installable .apk

I can't compile Android apps inside this chat (no SDK, no internet in this sandbox), so
here's the actual path — takes about 5 minutes, no Android Studio required:

1. Create a free GitHub account if you don't have one, and create a new **private** repo
   (e.g. `diary-ai`).
2. Upload everything in this zip to that repo (drag-and-drop on github.com works, or
   `git push` if you're comfortable with git).
3. GitHub will automatically start building the APK (there's a workflow file at
   `.github/workflows/build.yml` that does this on every push) — takes ~2-3 minutes.
4. Go to the repo's **Actions** tab → click the latest run → scroll down to **Artifacts** →
   download `DiaryAI-debug-apk` → unzip it → you have `app-debug.apk`.
5. Copy that `.apk` to your phone (email it to yourself, Google Drive, USB — anything) and
   tap it to install. You'll need to allow "install from unknown sources" the first time
   Android asks — that's normal for any APK not from the Play Store.

If you'd rather not use GitHub, opening the folder in Android Studio and hitting
**Build → Build APK(s)** does the same thing locally. Either way works — GitHub Actions
just means you don't need to install Android Studio at all.

## Voice input — now uses your phone's built-in Google voice typing

No models to download, no setup. Two things worth knowing:
- For **fully offline** recognition (works with no data connection), open the **Google app**
  on your phone → Settings → Voice → **Offline speech recognition** → download English
  (and Hindi, if you want it). Without that download it'll silently use mobile data/WiFi
  when available instead.
- Pick English or Hindi from the dropdown on the voice-note screen before you start
  recording — whichever you pick is what gets saved in the `language` column.

## What's fully implemented
- 4-option home menu, all 4 screens wired up.
- Local TSV storage layer (append, upsert-by-date, read-with-filter).
- Voice note capture: type auto-detected from your first spoken word (Diary/Idea/Thoughts),
  light auto-punctuation, and a **mandatory review-and-edit screen** before anything saves.
- Voice-based expense entry: heuristic parsing of amount/category/cash-online/whose-exp from
  a spoken sentence, editable fields, batch entry for logging several expenses in one go.
- Daily diary metrics form (one row/day; re-opens and edits today's row if already filled).
- 3-tab dashboard (Dashboard / Expenses / Diary) with Daily/Weekly/Monthly/Yearly/All-time/
  Custom range picker, pie/line/bar charts, tap-a-bar popup breakdown on Expenses, and
  year-month-grouped raw data cards on both Expenses and Diary tabs.
- Monthly A4 PDF export in your exact page order: dashboard summary → category-vs-date
  expenses → daily diary → Ideas → Thoughts (blank days skipped for Ideas/Thoughts).

## File layout
```
DiaryAI/
  .github/workflows/build.yml   — auto-builds the APK on push, no local setup needed
  app/src/main/java/com/arvind/diaryai/
    data/Schema.kt               — exact column definitions from your 2 forms
    data/StorageManager.kt       — local TSV read/write/upsert
    util/TranscriptProcessor.kt  — type detection + punctuation cleanup
    util/ExpenseVoiceParser.kt   — speech -> structured expense fields
    util/DateRange.kt            — daily/weekly/monthly/yearly/all-time/custom
    util/PdfExportUtil.kt        — monthly A4 PDF builder
    ui/*Activity.kt, ui/*Fragment.kt — all 4 screens + 3-tab dashboard
```
