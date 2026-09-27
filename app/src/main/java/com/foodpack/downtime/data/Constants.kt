package com.foodpack.downtime.data

/**
 * Everything about "what happened on the line" lives here in one place.
 * Add a new line, station, part, or reason by editing these lists —
 * nothing else in the app needs to change.
 */
object Constants {

    // Production lines
    val LINES = (1..6).map { "Line $it" }

    // Stations / equipment on a line
    val STATIONS = listOf(
        "Depositor",
        "Fish Knife Depositor",
        "Labeler",
        "Scissors",
        "Utensils / Scoop",
        "Material Supply (Prep)",
        "Other"
    )

    // Sub-parts, only shown when station == "Labeler"
    val LABELER_PARTS = listOf(
        "Roller",
        "White Transfer Belt",
        "Sponge Wheel (bottom label stick)",
        "Blade (glue build-up)",
        "Product Settings / Changeover",
        "Other"
    )

    // Sub-issues, only shown when station == "Material Supply (Prep)"
    val MATERIAL_SUB_ISSUES = listOf(
        "Prep delivered late",
        "Prep didn't make enough",
        "Wrong material issued",
        "Material shortage / ran out",
        "Other"
    )

    // Quick-tap reason chips, keyed by station name. "Other" station falls back to GENERAL_REASONS.
    val QUICK_REASONS: Map<String, List<String>> = mapOf(
        "Depositor" to listOf(
            "Clogged / jammed",
            "Weight / calibration off",
            "Mechanical fault",
            "Cleaning / changeover"
        ),
        "Fish Knife Depositor" to listOf(
            "Clogged / jammed",
            "Blade issue",
            "Weight / calibration off",
            "Mechanical fault",
            "Cleaning / changeover"
        ),
        "Labeler" to listOf(
            "Glue build-up on blade",
            "Label misfeed / jam",
            "Roller issue",
            "Transfer belt slipping / torn",
            "Sponge wheel not sticking label",
            "Changeover / settings issue",
            "Sensor / eye misread"
        ),
        "Scissors" to listOf(
            "Blade dull",
            "Jammed",
            "Mechanical fault"
        ),
        "Utensils / Scoop" to listOf(
            "Broken / worn",
            "Missing / not enough",
            "Wrong size for product"
        ),
        "Material Supply (Prep)" to listOf(
            "Prep delivered late",
            "Prep didn't make enough",
            "Wrong material issued",
            "Ran out mid-shift"
        )
    )

    val GENERAL_REASONS = listOf(
        "Power / electrical",
        "Cleaning / changeover",
        "Staffing",
        "Quality hold",
        "Other"
    )

    fun quickReasonsFor(station: String): List<String> =
        QUICK_REASONS[station] ?: GENERAL_REASONS
}
