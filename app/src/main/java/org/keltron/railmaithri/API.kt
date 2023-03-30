package org.keltron.railmaithri

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

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
        const val GENDER_TYPES                = "$BASE_URL/railmaithri/dropdown/gender_type_list/"
        const val CONTACT_TYPES               = "$BASE_URL/railmaithri/dropdown/contacts_category_list/"
        const val WATCH_ZONE_TYPES            = "$BASE_URL/railmaithri/dropdown/watchzone_category_list/"
        const val VENDOR_TYPES                = "$BASE_URL/railmaithri/dropdown/ua_vendor_beggar_list/"
        const val LOST_PROPERTY_TYPES         = "$BASE_URL/railmaithri/dropdown/lost_property_category_list/"
        const val FOUND_IN_TYPES              = "$BASE_URL/railmaithri/dropdown/found_in_type_list/"
        const val SURAKSHA_SAMITHI_LIST       = "$BASE_URL/railmaithri/dropdown/suraksha_samithi_list/"
        const val SHOP_TYPES                  = "$BASE_URL/railmaithri/dropdown/shop_category_list/"
        const val CRIME_MEMO_TYPES            = "$BASE_URL/api/v1/crime_memo_category/"

        const val INCIDENT_REPORT             = "$BASE_URL/api/v1/incident_report/"
        const val PASSENGER_STATISTICS        = "$BASE_URL/api/v1/passenger_statistics/"
        const val STRANGER_CHECK              = "$BASE_URL/api/v1/stranger_check/"
        const val BEAT_DIARY                  = "$BASE_URL/api/v1/beat_diary/"
        const val POI                         = "$BASE_URL/api/v1/poi/"
        const val EMERGENCY_CONTACTS          = "$BASE_URL/api/v1/contacts/"
        const val LOST_PROPERTY               = "$BASE_URL/api/v1/lost_property/"
        const val ABANDONED_PROPERTY          = "$BASE_URL/api/v1/abandoned_property/"
        const val RELIABLE_PERSON             = "$BASE_URL/api/v1/reliable_person/"
        const val INTELLIGENCE_INFORMATION    = "$BASE_URL/api/v1/intelligence_report/"

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

        fun postRequest(
            token: String,
            url: String,
            data: JSONObject,
            file: ByteArray?,
            fileName: String?
        ): Request {
            val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            if (file != null && fileName != null) {
                requestBody.addFormDataPart(
                    "file_upload",
                    fileName,
                    RequestBody.create("application/octet-stream".toMediaType(), file)
                )
            }
            for (key in data.keys()) {
                requestBody.addFormDataPart(key, data.get(key).toString())
            }
            return Request.Builder()
                .addHeader("Authorization", "Token $token")
                .url(url)
                .post(requestBody.build())
                .build()
        }
    }
}