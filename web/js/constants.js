// Same taxonomy as the Android app (data/Constants.kt). Edit here to add lines, categories, equipment or faults.

export const LINES = [1, 2, 3, 4, 5, 6].map(n => `Line ${n}`);
export const LOMA_MDS = [1, 2, 3, 4, 5, 6].map(n => `Loma MD ${n}`);

export const NOTE_FOLDERS = ["YCM Scales Settings", "Labeler Basic Checks", "Line Manning", "Production Plan"];
export const NOTE_FOLDER_ICONS = {
  "YCM Scales Settings": "⚖️", "Labeler Basic Checks": "🏷️", "Line Manning": "👥", "Production Plan": "📋",
};

export const CATEGORIES = ["Machinery", "Engineering", "Labour", "Materials", "Changeover", "Production Plan", "Quality", "Tray pack", "Other"];
export const CATEGORY_ICONS = {
  "Machinery": "⚙️", "Engineering": "🔧", "Labour": "👥", "Materials": "📦", "Changeover": "🔄",
  "Production Plan": "📋", "Quality": "✅", "Tray pack": "📥", "Other": "⚠️",
};

export const EQUIPMENT_BY_CATEGORY = {
  "Machinery": ["Labeler", "Depositor", "Fish Knife", "Utensils / Scoop", "Conveyor belt", "Printer",
    "Marco screen / system", "Sensors", "Scissors", "YCM Scales", "Other Machinery"],
};
export const EQUIPMENT_ICONS = {
  "Labeler": "🏷️", "Depositor": "🥣", "Fish Knife": "🔪", "Utensils / Scoop": "🥄", "Conveyor belt": "➡️",
  "Printer": "🖨️", "Marco screen / system": "🖥️", "Sensors": "📡", "Scissors": "✂️", "YCM Scales": "⚖️",
  "Other Machinery": "⚙️",
};

const FAULTS = {
  "Machinery|Labeler": [
    "Glue build-up on blade", "Blade not aligned / tension issue", "Label misfeed / jam", "Label not cutting straight",
    "Roller loose / worn", "Orange drive roller worn or broken", "Blue drive roller worn or broken",
    "White roller glue build-up", "Transfer belt slipping / torn", "Transfer belt labels not sticking",
    "Orange sponge wheel damaged", "Sensor / eye misread", "Air leak / pneumatic issue",
    "Settings / changeover issue", "Blade cleaning", "Blade tension",
  ],
  "Machinery|Depositor": ["Clogged / jammed", "Not dispensing / dispersing", "Weight / calibration off", "Awaiting cleaning / wash", "Mechanical fault"],
  "Machinery|Fish Knife": ["Clogged / jammed", "Blade issue", "Weight / calibration off", "Mechanical fault"],
  "Machinery|Utensils / Scoop": ["Broken / worn", "Missing / not enough", "Wrong size for product"],
  "Machinery|Conveyor belt": ["Conveyor belt issue / wrong mode", "Conveyor jam", "Mechanical fault"],
  "Machinery|Printer": ["Date / time not printing clearly", "Printer not connecting", "Printer head issue / air leak", "Ribbon or cartridge issue", "Printer screen fault", "Marco screen frozen"],
  "Machinery|Marco screen / system": ["Screen frozen", "System not responding", "Needs restart", "Wrong product / settings loaded"],
  "Machinery|Sensors": ["Sensor needs re-teaching", "Sensor dirty / needs cleaning", "Sensor misaligned", "Reflector dirty / needs cleaning"],
  "Machinery|Scissors": ["Blade dull", "Jammed", "Mechanical fault"],
  "Machinery|YCM Scales": ["Not working", "Inaccurate / needs calibration", "Mechanical fault"],
  "Machinery|Other Machinery": ["Breakdown", "Cleaning required", "Air leak / pneumatic issue"],
  "Engineering": ["Waiting for engineer", "Engineer not on site", "Parts not available", "No engineering cover", "5am Eng present: Yes", "5am Eng present: No"],
  "Labour": ["Staffing shortage", "Absence", "Agency late", "Training", "Break / rotation delay", "Skill issue", "Union late"],
  "Materials": ["Prep delivered late", "Prep didn't make enough", "Wrong material issued", "Missing ingredient", "Ran out mid-shift", "Packaging store delay"],
  "Changeover": ["Product settings changeover", "Cleaning changeover", "Line setup / readiness"],
  "Production Plan": ["Plan change", "Order change", "Sequence change", "Changeover time inaccurate", "Wrong allergen order"],
  "Quality": ["Product on hold", "Rework", "Inspection", "QAS Camera verification time"],
  "Tray pack": ["Waiting for tray pack", "Not enough trays from despatch", "Storage / logistics issue", "Metal hit on Loma, waiting for QAS", "Printer issue", "Fire alarm"],
  "Other": ["Power cut / electrical", "Fire alarm"],
};

export const hasEquipmentStep = c => Object.prototype.hasOwnProperty.call(EQUIPMENT_BY_CATEGORY, c);
export const equipmentFor = c => EQUIPMENT_BY_CATEGORY[c] || [];
export const faultsFor = (category, equipment) =>
  [...(FAULTS[equipment ? `${category}|${equipment}` : category] || []), "Other"];

export const TEAMS = ["Team A", "Team B", "Team C"];
export const BREAK_TYPES = ["Morning", "Afternoon"];
