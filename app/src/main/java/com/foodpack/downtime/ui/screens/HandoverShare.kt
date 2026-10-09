package com.foodpack.downtime.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.foodpack.downtime.data.HandoverEntry
import com.foodpack.downtime.data.HandoverType

/** Builds the plain-text handover message and hands it to WhatsApp's share sheet. */
object HandoverShare {

    private fun yn(flag: Boolean, detail: String): String =
        if (flag) "Y" + if (detail.isNotBlank()) " - $detail" else "" else "N"

    private fun n(v: Int?): String = v?.toString() ?: "-"

    private fun team(name: String, plan: Int?, act: Int?, fp: String, fa: String): String =
        "$name:  planned ${n(plan)}  actual ${n(act)}\nFinish time: planned ${fp.ifBlank { "-" }}  actual ${fa.ifBlank { "-" }}"

    fun text(e: HandoverEntry): String =
        if (e.type == HandoverType.MORNING) morning(e) else evening(e)

    private fun morning(e: HandoverEntry): String = buildString {
        appendLine("*Morning Update* - ${e.date}")
        appendLine()
        appendLine("In the last 24hrs:")
        appendLine("SHE incidents and accidents: ${yn(e.sheIncident, e.sheIncidentDetail)}")
        appendLine("Technical: ${yn(e.technicalIssue, e.technicalDetail)}")
        appendLine("Quality: ${yn(e.qualityIssue, e.qualityDetail)}")
        appendLine("Downtime: ${yn(e.downtimeIssue, e.downtimeDetail)}")
        appendLine("Today's Volume: ${n(e.todaysVolume)}")
        appendLine("Tomorrow's Volume: ${n(e.tomorrowsVolume)}")
        appendLine()
        appendLine(team("Team A", e.teamAPlanned, e.teamAActual, e.teamAFinishPlanned, e.teamAFinishActual))
        appendLine(team("Team B", e.teamBPlanned, e.teamBActual, e.teamBFinishPlanned, e.teamBFinishActual))
        appendLine(team("Team C", e.teamCPlanned, e.teamCActual, e.teamCFinishPlanned, e.teamCFinishActual))
        appendLine("Packing: ${n(e.packingPpl)} ppl + ${n(e.packingTrainees)} training")
        appendLine()
        appendLine("Absence: ${yn(e.absence, e.absenceNames)}")
        appendLine("RTW: ${yn(e.rtw, e.rtwNames)}")
        appendLine("Not on rota: ${yn(e.notOnRota, e.notOnRotaNames)}")
        appendLine()
        appendLine("Risk to Service: ${yn(e.riskToService, e.riskToServiceDetail)}")
        appendLine()
        appendLine("Agency: ${n(e.agencyCollation)} Collation, ${n(e.agencyPrep)} Prep, ${n(e.agencyHygiene)} Hygiene, ${n(e.agencyFridge)} Fridge, ${n(e.agencyPackagingStore)} Packaging Store")
        appendLine()
        appendLine("Thank you")
        append(e.operatorName)
    }

    private fun evening(e: HandoverEntry): String = buildString {
        appendLine("*End of Shift Update* - ${e.date}")
        appendLine()
        appendLine("Accident/incident: ${yn(e.accidentIncident, e.accidentIncidentDetail)}")
        appendLine("Quality: ${yn(e.qualityIssue, e.qualityDetail)}")
        appendLine("Downtime: ${yn(e.downtimeIssue, e.downtimeDetail)}")
        appendLine("Service score: ${n(e.serviceScore)}%")
        appendLine("Efficiency: ${n(e.efficiency)}%")
        appendLine()
        appendLine("Team A finished: ${e.teamAFinishedTime.ifBlank { "-" }}")
        appendLine("Team B finished: ${e.teamBFinishedTime.ifBlank { "-" }}")
        appendLine("Team C finished: ${e.teamCFinishedTime.ifBlank { "-" }}")
        appendLine("Collation finished: ${e.collationFinished.ifBlank { "-" }}")
        appendLine("Tray up sent by: ${e.trayUpSentBy.ifBlank { "-" }}")
        appendLine()
        appendLine("Shorts: ${yn(e.shorts, e.shortsDetail)}")
        appendLine("Report updated: ${yn(e.reportUpdated, e.reportUpdatedDetail)}")
        appendLine("AOB: ${yn(e.aob, e.aobDetail)}")
        appendLine()
        appendLine("Regards")
        append(e.operatorName)
    }

    /** Opens WhatsApp (or WhatsApp Business) with the text pre-filled; falls back to the system share sheet. */
    fun shareToWhatsApp(context: Context, message: String) {
        fun send(pkg: String?): Boolean {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                if (pkg != null) setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try { context.startActivity(intent); true } catch (e: ActivityNotFoundException) { false }
        }
        if (send("com.whatsapp")) return
        if (send("com.whatsapp.w4b")) return
        val chooser = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, message) },
            "Share handover"
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
