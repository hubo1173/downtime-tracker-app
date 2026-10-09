package com.foodpack.downtime.data

/** Rolls the downtime log up into totals and "top causes" for handovers and the weekly report. */
object DowntimeSummary {

    data class Group(val label: String, val minutes: Int, val people: Int, val stops: Int)

    data class Result(
        val totalMinutes: Int,
        val stops: Int,
        val peopleAffected: Int,
        val byCategory: List<Group>,
        val topCauses: List<Group>
    )

    private fun minutesOf(e: DowntimeEntry, now: Long): Int =
        e.durationMinutes
            ?: if (e.isOpen) ((now - (e.startTime ?: e.createdAt)) / 60_000L).toInt().coerceAtLeast(0) else 0

    private fun cause(e: DowntimeEntry): String =
        if (e.part.isNullOrBlank()) "${e.station}: ${e.reason}" else "${e.part}: ${e.reason}"

    private fun group(entries: List<DowntimeEntry>, now: Long, key: (DowntimeEntry) -> String): List<Group> =
        entries.groupBy(key).map { (k, list) ->
            Group(k, list.sumOf { minutesOf(it, now) }, list.sumOf { it.peopleAffected ?: 0 }, list.size)
        }.sortedWith(compareByDescending<Group> { it.minutes }.thenByDescending { it.stops })

    fun since(all: List<DowntimeEntry>, sinceMillis: Long, now: Long = System.currentTimeMillis()): Result {
        val list = all.filter { it.createdAt >= sinceMillis && !it.pendingDelete }
        return Result(
            totalMinutes = list.sumOf { minutesOf(it, now) },
            stops = list.size,
            peopleAffected = list.sumOf { it.peopleAffected ?: 0 },
            byCategory = group(list, now) { it.station },
            topCauses = group(list, now, ::cause)
        )
    }

    /** One-paragraph text for the handover "Downtime" detail box. Empty if nothing was logged. */
    fun handoverText(r: Result): String {
        if (r.stops == 0) return ""
        val top = r.topCauses.take(3).joinToString("; ") { "${it.label} ${it.minutes} min" }
        val ppl = if (r.peopleAffected > 0) ", ${r.peopleAffected} ppl affected" else ""
        return "${r.totalMinutes} min lost across ${r.stops} stop${if (r.stops == 1) "" else "s"}$ppl. Top: $top"
    }

    /** WhatsApp-ready summary for the weekly / monthly report. */
    fun reportText(title: String, r: Result): String = buildString {
        appendLine("*$title*")
        appendLine("Total: ${r.totalMinutes} min lost, ${r.stops} stops, ${r.peopleAffected} people affected")
        appendLine()
        appendLine("By category:")
        r.byCategory.forEach { appendLine("- ${it.label}: ${it.minutes} min, ${it.people} ppl, ${it.stops} stops") }
        appendLine()
        appendLine("Top causes:")
        r.topCauses.take(5).forEachIndexed { i, g -> appendLine("${i + 1}. ${g.label}: ${g.minutes} min (${g.stops}x)") }
    }.trimEnd()
}
