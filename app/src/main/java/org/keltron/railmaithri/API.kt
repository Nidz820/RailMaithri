package org.keltron.railmaithri

import okhttp3.MultipartBody
import okhttp3.Request

class API {
    companion object {
        private const val DEVELOPMENT_URL = "http://192.168.4.63:8000"
        private const val DEPLOYMENT_URL  = "http://103.10.168.42:8000"
        private const val BASE_URL        = DEVELOPMENT_URL

        
        const val RAILWAY_STATIONS_LIST       = "$BASE_URL/railmaithri/dropdown/railway_station_list/"
        const val TRAINS_LIST                 = "$BASE_URL/railmaithri/dropdown/train_list/"
        const val INTELLIGENCE_SEVERITY_TYPES = "$BASE_URL/railmaithri/dropdown/severity_type_list/"
        const val INTELLIGENCE_TYPES          = "$BASE_URL/railmaithri/dropdown/intelligence_type_list/"
        const val COMPARTMENT_TYPES           = "$BASE_URL/railmaithri/dropdown/compartment_type_list/"
        const val DENSITY_TYPES               = "$BASE_URL/railmaithri/dropdown/density_category_list/"
        const val MEETING_TYPES               = "$BASE_URL/railmaithri/dropdown/janamaithri_meeting_type_list/"
        const val POI_TYPES                   = "$BASE_URL/railmaithri/dropdown/poi_category_list/"
        const val POLICE_STATIONS_LIST        = "$BASE_URL/accounts/dropdown/police_station_list/"
        const val DISTRICTS_LIST              = "$BASE_URL/accounts/dropdown/district_list/"
        const val STATES_LIST                 = "$BASE_URL/accounts/dropdown/states/"
        const val ABANDONED_PROPERTY_TYPES    = "$BASE_URL/railmaithri/dropdown/abandoned_property_category_list/"
        const val RAIL_VOLUNTEER_TYPES        = "$BASE_URL/railmaithri/dropdown/rail_volunteer_category_list/"
        const val GENDER_TYPES                = "$BASE_URL/api/v1/gender_type/"
        const val CONTACT_TYPES               = "$BASE_URL/railmaithri/dropdown/contacts_category_list/"
        const val WATCH_ZONE_TYPES            = "$BASE_URL/railmaithri/dropdown/watchzone_category_list/"
        const val VENDOR_TYPES                = "$BASE_URL/railmaithri/dropdown/ua_vendor_beggar_list/"
        const val LOST_PROPERTY_TYPES         = "$BASE_URL/railmaithri/dropdown/lost_property_category_list/"
        const val FOUND_IN_TYPES              = "$BASE_URL/api/v1/found_in_type/"
        const val SURAKSHA_SAMITHI_LIST       = "$BASE_URL/railmaithri/dropdown/suraksha_samithi_list/"
        const val SHOP_TYPES                  = "$BASE_URL/railmaithri/dropdown/shop_category_list/"
        const val CRIME_MEMO_TYPES            = "$BASE_URL/api/v1/crime_memo_category/"

        fun loginRequest(username: String, password: String): Request {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("username", username)
                .addFormDataPart("password", password)
                .build()
            return Request.Builder()
                .url("$BASE_URL/accounts/mobile_login/")
                .method("POST", body)
                .build()
        }

        fun getRequest(url: String, token: String):Request {
            return Request.Builder()
                .addHeader("Authorization", "Token $token")
                .url(url)
                .build()
        }
    }
}