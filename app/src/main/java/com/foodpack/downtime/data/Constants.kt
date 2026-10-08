package com.foodpack.downtime.data

/**
 * Everything about "what stopped the line" lives here in one place.
 *
 * Taxonomy: Line -> Category -> Equipment (Machinery only) -> Fault.
 * A category other than Machinery skips straight from Category to Fault
 * (no equipment step) — see [equipmentFor] and [faultsFor].
 *
 * Add a new line, category, piece of equipment, or fault by editing the
 * lists below — nothing else in the app needs to change.
 */
object Constants {

    // Production lines
    val LINES = (1..6).map { "Line $it" }

    // Notes section: fixed Drive subfolders an operator can attach a
    // reference file (settings sheet, calibration doc, etc.) to. Keep
    // this in sync with NOTE_FOLDERS in apps-script/Code.gs.
    val NOTE_FOLDERS = listOf(
        "YCM Scales Settings",
        "Labeler Basic Checks",
        "Line Manning",
        "Production Plan"
    )

    val NOTE_FOLDER_ICONS: Map<String, String> = mapOf(
        "YCM Scales Settings" to "⚖️",
        "Labeler Basic Checks" to "🏷️",
        "Line Manning" to "👥",
        "Production Plan" to "📋"
    )

    // Top-level categories — what kind of thing caused the stop.
    // Based on recurring patterns in the Unit K downtime log: labeller/blade/glue
    // issues are the single biggest driver, followed by engineer response time,
    // staffing/agency, and materials/planning.
    val CATEGORIES = listOf(
        "Machinery",
        "Engineering",
        "Labour",
        "Materials",
        "Changeover",
        "Production Plan",
        "Quality",
        "Warehouse",
        "Other"
    )

    // A small icon shown next to each category chip — purely cosmetic.
    val CATEGORY_ICONS: Map<String, String> = mapOf(
        "Machinery" to "⚙️",
        "Engineering" to "🔧",
        "Labour" to "👥",
        "Materials" to "📦",
        "Changeover" to "🔄",
        "Production Plan" to "📋",
        "Quality" to "✅",
        "Warehouse" to "🏭",
        "Other" to "⚠️"
    )

    // Only Machinery breaks down further into specific equipment.
    val EQUIPMENT_BY_CATEGORY: Map<String, List<String>> = mapOf(
        "Machinery" to listOf(
            "Labeler",
            "Depositor",
            "Fish Knife",
            "Utensils / Scoop",
            "Conveyor belt",
            "Printer",
            "Sensors",
            "Scissors",
            "YCM Scales",
            "Other Machinery"
        )
    )

    val EQUIPMENT_ICONS: Map<String, String> = mapOf(
        "Labeler" to "🏷️",
        "Depositor" to "🥣",
        "Fish Knife" to "🔪",
        "Utensils / Scoop" to "🥄",
        "Conveyor belt" to "➡️",
        "Printer" to "🖨️",
        "Sensors" to "📡",
        "Scissors" to "✂️",
        "YCM Scales" to "⚖️",
        "Other Machinery" to "⚙️"
    )

    fun hasEquipmentStep(category: String): Boolean =
        EQUIPMENT_BY_CATEGORY.containsKey(category)

    fun equipmentFor(category: String): List<String> =
        EQUIPMENT_BY_CATEGORY[category] ?: emptyList()

    // Fault chips, keyed by "Category" for categories with no equipment step,
    // or "Category|Equipment" for Machinery.
    private val FAULTS: Map<String, List<String>> = mapOf(
        "Machinery|Labeler" to listOf(
            "Glue build-up on blade",
            "Blade not aligned / tension issue",
            "Label misfeed / jam",
            "Label not cutting straight",
            "Roller loose / worn",
            "Orange drive roller worn or broken",
            "Blue drive roller worn or broken",
            "White roller glue build-up",
            "Transfer belt slipping / torn",
            "Transfer belt labels not sticking",
            "Orange sponge wheel damaged",
            "Sensor / eye misread",
            "Air leak / pneumatic issue",
            "Settings / changeover issue"
        ),
        "Machinery|Depositor" to listOf(
            "Clogged / jammed",
            "Not dispensing / dispersing",
            "Weight / calibration off",
            "Awaiting cleaning / wash",
            "Mechanical fault"
        ),
        "Machinery|Fish Knife" to listOf(
            "Clogged / jammed",
            "Blade issue",
            "Weight / calibration off",
            "Mechanical fault"
        ),
        "Machinery|Utensils / Scoop" to listOf(
            "Broken / worn",
            "Missing / not enough",
            "Wrong size for product"
        ),
        "Machinery|Conveyor belt" to listOf(
            "Conveyor belt issue / wrong mode",
            "Conveyor jam",
            "Mechanical fault"
        ),
        "Machinery|Printer" to listOf(
            "Date / time not printing clearly",
            "Printer not connecting",
            "Printer head issue / air leak",
            "Ribbon or cartridge issue",
            "Printer screen fault",
            "Marco screen frozen"
        ),
        "Machinery|Sensors" to listOf(
            "Sensor needs re-teaching",
            "Sensor dirty / needs cleaning",
            "Sensor misaligned",
            "Reflector dirty / needs cleaning"
        ),
        "Machinery|Scissors" to listOf(
            "Blade dull",
            "Jammed",
            "Mechanical fault"
        ),
        "Machinery|YCM Scales" to listOf(
            "Not working",
            "Inaccurate / needs calibration",
            "Mechanical fault"
        ),
        "Machinery|Other Machinery" to listOf(
            "Breakdown",
            "Cleaning required",
            "Air leak / pneumatic issue"
        ),
        "Engineering" to listOf(
            "Waiting for engineer",
            "Engineer not on site",
            "Parts not available",
            "No engineering cover"
        ),
        "Labour" to listOf(
            "Staffing shortage",
            "Absence",
            "Agency late",
            "Training",
            "Break / rotation delay"
        ),
        "Materials" to listOf(
            "Prep delivered late",
            "Prep didn't make enough",
            "Wrong material issued",
            "Missing ingredient",
            "Ran out mid-shift",
            "Tray pack packaging issued late"
        ),
        "Changeover" to listOf(
            "Product settings changeover",
            "Cleaning changeover",
            "Line setup / readiness"
        ),
        "Production Plan" to listOf(
            "Plan change",
            "Order change",
            "Sequence change"
        ),
        "Quality" to listOf(
            "Quality hold",
            "Rework",
            "Inspection",
            "Camera verification / QAS issue"
        ),
        "Warehouse" to listOf(
            "Waiting on warehouse",
            "Storage / logistics issue"
        ),
        "Other" to listOf(
            "Power / electrical"
        )
    )

    /** Faults for a category with no equipment step (e.g. "Materials"). */
    fun faultsFor(category: String): List<String> =
        (FAULTS[category] ?: emptyList()) + "Other"

    /** Faults for a category+equipment pair (Machinery only). */
    fun faultsFor(category: String, equipment: String): List<String> =
        (FAULTS["$category|$equipment"] ?: emptyList()) + "Other"
}
