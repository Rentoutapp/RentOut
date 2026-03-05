package org.example.project.ui.navigation

object NavRoutes {
    // Auth flow
    const val INTRO          = "intro"
    const val ROLE_SELECTION = "role_selection"
    const val AUTH           = "auth?prefillEmail={prefillEmail}&prefillPassword={prefillPassword}"
    const val SPLASH         = "splash"
    const val SUSPENDED      = "suspended"

    // Landlord flow
    const val LANDLORD_DASHBOARD = "landlord_dashboard"
    const val ADD_PROPERTY       = "add_property"
    const val PROPERTY_IMAGES    = "property_images"
    const val EDIT_PROPERTY      = "edit_property/{propertyId}"
    const val LANDLORD_PROFILE   = "landlord_profile"

    // Tenant flow
    const val TENANT_HOME        = "tenant_home"
    const val PROPERTY_DETAIL    = "property_detail/{propertyId}"
    const val PAYMENT            = "payment/{propertyId}"
    const val UNLOCKED_PROPERTIES= "unlocked_properties"
    const val TENANT_PROFILE     = "tenant_profile"

    // Helpers
    fun editProperty(propertyId: String) = "edit_property/$propertyId"
    fun propertyDetail(propertyId: String) = "property_detail/$propertyId"
    fun payment(propertyId: String) = "payment/$propertyId"
    fun authWithPrefill(email: String, password: String) =
        "auth?prefillEmail=${encodeArg(email)}&prefillPassword=${encodeArg(password)}"

    private fun encodeArg(value: String) =
        value.replace("@", "%40").replace("+", "%2B").replace(" ", "%20")
}
