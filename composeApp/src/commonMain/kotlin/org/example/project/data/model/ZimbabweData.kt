package org.example.project.data.model

// ─── SADC Countries ───────────────────────────────────────────────────────────
data class SadcCountry(val name: String, val flag: String, val dialCode: String)

val SADC_COUNTRIES = listOf(
    SadcCountry("Zimbabwe",         "🇿🇼", "+263"),
    SadcCountry("South Africa",     "🇿🇦", "+27"),
    SadcCountry("Zambia",           "🇿🇲", "+260"),
    SadcCountry("Botswana",         "🇧🇼", "+267"),
    SadcCountry("Mozambique",       "🇲🇿", "+258"),
    SadcCountry("Namibia",          "🇳🇦", "+264"),
    SadcCountry("Tanzania",         "🇹🇿", "+255"),
    SadcCountry("Malawi",           "🇲🇼", "+265"),
    SadcCountry("Angola",           "🇦🇴", "+244"),
    SadcCountry("Lesotho",          "🇱🇸", "+266"),
    SadcCountry("Eswatini",         "🇸🇿", "+268"),
    SadcCountry("Madagascar",       "🇲🇬", "+261"),
    SadcCountry("Mauritius",        "🇲🇺", "+230"),
    SadcCountry("Democratic Republic of Congo", "🇨🇩", "+243"),
    SadcCountry("Seychelles",       "🇸🇨", "+248"),
    SadcCountry("Comoros",          "🇰🇲", "+269")
)

// ─── Zimbabwe Towns & Their Suburbs ──────────────────────────────────────────

data class ZimTown(val name: String, val suburbs: List<String>)

val ZIMBABWE_TOWNS: List<ZimTown> = listOf(

    ZimTown("Gweru", listOf(
        "Adelaide Park", "Ascot", "Ascot Extension", "Athlone",
        "Babourfields", "Boggie", "Bridgewood (Mega City)", "Chachacha",
        "Charlton Park", "Claremont Park", "Clifton Park", "Clydesdale",
        "Daylesford", "Donnington", "Gweru East", "Gwelo Estate",
        "Harben Park", "Hertfordshire Phase 1", "Hertfordshire Phase 2", "Ivene", "Kopje",
        "Lynfield (Lingfield)", "Lundi Park", "Mambo", "Mkoba",
        "Mkoba (Sections 1 through 21)", "Montrose", "Mtausi Park",
        "Mtapa", "Municipal Area", "Munhumutapa", "Nashville",
        "Nehanda", "Nehosho", "New Christmas Gift", "Northgate Heights",
        "Northlea", "Randolph", "Rhode", "Ridgemont", "Riverside",
        "Tatenda Park",
        "Senga", "Shamrock Park", "Southdowns", "Southview",
        "Thornhill", "Tinshel", "Westview Estate", "Windsor Park",
        "Woodlands", "Woodlands Park", "Warren Park"
    )),

    ZimTown("Harare", listOf(
        "Avondale", "Avenues", "Belgravia", "Borrowdale", "Braeside",
        "Budiriro", "Chitungwiza", "Dzivarasekwa", "Epworth", "Glen Norah",
        "Glen View", "Greendale", "Groombridge", "Gun Hill", "Hatfield",
        "Highfield", "Highlands", "Hillside", "Hopley", "Kuwadzana",
        "Kambuzuma", "Larimer", "Mabvuku", "Mbare", "Mount Pleasant",
        "Msasa", "Mufakose", "Newlands", "Prospect", "Queensdale",
        "Ruwa", "Southerton", "Sunningdale", "Tafara", "Warren Park",
        "Waterfalls", "Westgate", "Zengeza", "Dzivaresekwa", "Marlborough"
    )),

    ZimTown("Bulawayo", listOf(
        "Barbourfields", "Bellevue", "Cowdray Park", "Entumbane",
        "Famona", "Gwabalanda", "Hillside", "Iminyela", "Khumalo",
        "Lobengula", "Luveve", "Makokoba", "Malindela", "Mzilikazi",
        "Nketa", "Nkulumane", "Njube", "Pumula", "Queenspark",
        "Richmond", "Riverside", "Selbourne", "Sizinda", "Suburbs",
        "Sunninghill", "Tshabalala", "Velachitedza", "Woodville", "Emganwini"
    )),

    ZimTown("Mutare", listOf(
        "Alderdice", "Chikanga", "Dangamvura", "Fairbridge", "Fern Valley",
        "Hobhouse", "Hobhouse West", "Kudzanai", "Murambi", "Mutare CBD",
        "Nyakamete", "Palmerstone", "Sakubva", "Tiger's Kloof", "Township",
        "Zimta", "Yeovil", "Greenside", "Morningside", "Weirmouth"
    )),

    ZimTown("Masvingo", listOf(
        "Mucheke", "Runyararo", "Rujeko", "Victoria Ranch", "Rhodene",
        "Pangolin", "Masvingo CBD", "Silveira", "Longdale", "Zimuto"
    )),

    ZimTown("Kwekwe", listOf(
        "Amaveni", "Basket", "Globe and Phoenix", "Mbizo", "Newtown",
        "Rimuka", "Redcliff", "Torwood", "Zhombe"
    )),

    ZimTown("Chinhoyi", listOf(
        "Chikonohono", "Chinhoyi CBD", "Coon Section", "Hunyani Hills",
        "Madziva", "Mhangura", "Mikusha", "New Stands", "Old Stands"
    )),

    ZimTown("Bindura", listOf(
        "Chipadze", "Chiwaridzo", "Kimberley Reef", "Bindura CBD",
        "Aerodrome", "Chiconono", "Matepatepa"
    )),

    ZimTown("Zvishavane", listOf(
        "Mandava", "Mnene", "Zvishavane CBD", "Maglas", "Shabani"
    )),

    ZimTown("Chiredzi", listOf(
        "Mkwasine", "Chiredzi CBD", "Ngundu", "Triangle"
    )),

    ZimTown("Victoria Falls", listOf(
        "Chinotimba", "Mkhosana", "Victoria Falls CBD", "Wilderness"
    )),

    ZimTown("Hwange", listOf(
        "Baobab", "Colliery", "Empumalanga", "Hwange CBD", "North Side"
    )),

    ZimTown("Kariba", listOf(
        "Mahombekombe", "Kariba Heights", "Kariba CBD", "Nyamhunga"
    )),

    ZimTown("Kadoma", listOf(
        "Allandale", "Eiffel Flats", "Kadoma CBD", "Mbizo", "Rimuka"
    )),

    ZimTown("Gokwe", listOf(
        "Gokwe CBD", "Nembudziya", "Tonhorai"
    )),

    ZimTown("Rusape", listOf(
        "Rusape CBD", "Vengere", "Waterfall"
    )),

    ZimTown("Beitbridge", listOf(
        "Beitbridge CBD", "Cross-Border", "New Stands"
    )),

    ZimTown("Plumtree", listOf(
        "Plumtree CBD", "Somvelo"
    )),

    ZimTown("Norton", listOf(
        "Dorset", "Norton CBD", "Tichagarika"
    )),

    ZimTown("Marondera", listOf(
        "Cherutombo", "Dombotombo", "Marondera CBD", "Rudhaka"
    ))
)

// Convenience: look up suburbs by town name (case-insensitive)
fun suburbsForTown(townName: String): List<String> {
    return ZIMBABWE_TOWNS.firstOrNull {
        it.name.equals(townName, ignoreCase = true)
    }?.suburbs?.sorted() ?: emptyList()
}
