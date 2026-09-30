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

    // Top-level categories — what kind of thing caused the stop.
    val CATEGORIES = listOf(
        "Machinery",
        "Labour",
        "Materials",
        "Changeover",
        "Production Plan",
        "Quality",
        "Warehouse",
        "Other"
    )

    // Only Machinery breaks down further into specific equipment.
    val EQUIPMENT_BY_CATEGORY: Map<String, List<String>> = mapOf(
        "Machinery" to listOf(
            "Labeler",
            "Depositor",
            "Fish Knife Depositor",
            "Scissors",
            "Utensils / Scoop",
            "Other Machinery"
        )
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
            "Blade not aligned",
            "Label misfeed / jam",
            "Roller issue",
            "Transfer belt slipping / torn",
            "Sponge wheel not sticking label",
            "Sensor / eye misread",
            "Settings / changeover issue"
        ),
        "Machinery|Depositor" to listOf(
            "Clogged / jammed",
            "Weight / calibration off",
            "Mechanical fault"
        ),
        "Machinery|Fish Knife Depositor" to listOf(
            "Clogged / jammed",
            "Blade issue",
            "Weight / calibration off",
            "Mechanical fault"
        ),
        "Machinery|Scissors" to listOf(
            "Blade dull",
            "Jammed",
            "Mechanical fault"
        ),
        "Machinery|Utensils / Scoop" to listOf(
            "Broken / worn",
            "Missing / not enough",
            "Wrong size for product"
        ),
        "Machinery|Other Machinery" to listOf(
            "Breakdown",
            "Cleaning required"
        ),
        "Labour" to listOf(
            "Staffing shortage",
            "Absence",
            "Training",
            "Break / rotation delay"
        ),
        "Materials" to listOf(
            "Prep delivered late",
            "Prep didn't make enough",
            "Wrong material issued",
            "Ran out mid-shift"
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
            "Inspection"
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
