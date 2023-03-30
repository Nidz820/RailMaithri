package org.keltron.railmaithri

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

class EmergencyContacts: AppCompatActivity() {

    private lateinit var progressPB:         ProgressBar
    private lateinit var saveBT:             Button
    private lateinit var locationUtil:       LocationUtil
    private lateinit var policeStationSP:    Spinner
    private lateinit var railwayStationSP:   Spinner
    private lateinit var districtSP:         Spinner
    private lateinit var contactsCategorySP: Spinner

    private lateinit var nameET:             EditText
    private lateinit var contactNumberET:    EditText
    private lateinit var remarksET:          EditText

    private lateinit var policeStationAP:    ArrayAdapter<String>
    private lateinit var railwayStationAP:   ArrayAdapter<String>
    private lateinit var districtAP:         ArrayAdapter<String>
    private lateinit var contactsCategoryAP: ArrayAdapter<String>

    private lateinit var mode:               String
    private lateinit var railwayStations:    JSONArray
    private lateinit var districts:          JSONArray
    private lateinit var policeStations:     JSONArray
    private lateinit var contactCategories:  JSONArray
    private lateinit var utcTime:            String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.emergency_contacts)
        supportActionBar!!.hide()

        utcTime            = Helper.getUTC()
        mode               = intent.getStringExtra("mode")!!
        locationUtil       = LocationUtil(this, findViewById(R.id.ly_location))
        progressPB         = findViewById(R.id.progress_bar)
        saveBT             = findViewById(R.id.save)
        policeStationSP    = findViewById(R.id.police_station)
        railwayStationSP   = findViewById(R.id.railway_station)
        districtSP         = findViewById(R.id.district)
        contactsCategorySP = findViewById(R.id.contacts_category)
        nameET             = findViewById(R.id.name)
        contactNumberET    = findViewById(R.id.mobile_number)
        remarksET          = findViewById(R.id.remarks)

        railwayStations  = JSONArray(Helper.getData(this, Scope.RAILWAY_STATIONS_LIST)!!)
        railwayStationAP = Helper.makeArrayAdapter(railwayStations, this)
        railwayStationSP.adapter = railwayStationAP

        policeStations  = JSONArray(Helper.getData(this, Scope.POLICE_STATIONS_LIST)!!)
        policeStationAP = Helper.makeArrayAdapter(policeStations, this)
        policeStationSP.adapter = policeStationAP

        districts  = JSONArray(Helper.getData(this, Scope.DISTRICTS_LIST)!!)
        districtAP = Helper.makeArrayAdapter(districts, this)
        districtSP.adapter = districtAP

        contactCategories  = JSONArray(Helper.getData(this, Scope.CONTACT_TYPES)!!)
        contactsCategoryAP = Helper.makeArrayAdapter(contactCategories, this)
        contactsCategorySP.adapter = contactsCategoryAP

        progressPB.visibility = View.GONE
        saveBT.setOnClickListener {
            val inputData = validateInput()
            inputData?.let {
                progressPB.visibility = View.VISIBLE
                saveBT.isClickable = false
                CoroutineScope(Dispatchers.IO).launch {
                    sendForm(inputData)
                    Handler(Looper.getMainLooper()).post {
                        saveBT.isClickable = true
                        progressPB.visibility = View.GONE
                    }
                }
            }
        }

        if (mode == Scope.MODE_VIEW_FORM) {
            saveBT.visibility = View.GONE
        } else if (mode == Scope.MODE_UPDATE_FORM) {
            val savedData = intent.getStringExtra("saved_data")
            val formData  = JSONObject(savedData!!)
            utcTime = formData.getString("utc_timestamp")
            populateForm(formData)
        }
    }

    private fun populateForm(data: JSONObject) {
        nameET.setText(data.getString("name"))
        contactNumberET.setText(data.getString("contact_number"))
        remarksET.setText(data.getString("remarks"))

        val policeStationNumber    = data.getInt("police_station")
        val policeStationName      = Helper.getName(policeStations, policeStationNumber)
        val policeStationNumberPos = policeStationAP.getPosition(policeStationName)
        policeStationSP.setSelection(policeStationNumberPos)

        val railwayStationID   = data.getInt("railway_station")
        val railwayStationName = Helper.getName(railwayStations, railwayStationID)
        val railwayStationPos  = railwayStationAP.getPosition(railwayStationName)
        railwayStationSP.setSelection(railwayStationPos)

        val districtNumber    = data.getInt("district")
        val districtName      = Helper.getName(districts, districtNumber)
        val districtNumberPos = districtAP.getPosition(districtName)
        districtSP.setSelection(districtNumberPos)

        val contactCategoryNumber     = data.getInt("contacts_category")
        val contactsCategoryName      = Helper.getName(contactCategories, contactCategoryNumber)
        val contactsCategoryNumberPos = contactsCategoryAP.getPosition(contactsCategoryName)
        contactsCategorySP.setSelection(contactsCategoryNumberPos)
    }

    private fun validateInput(): JSONObject? {
        val name = nameET.text.toString()
        val contactNumber = contactNumberET.text.toString()
        val remarks = remarksET.text.toString()

        val policeStationNumberPos = policeStationSP.selectedItemPosition
        val policeStationNumber =
            policeStations.getJSONObject(policeStationNumberPos).getString("id").toString()

        val railwayStationNumberPos = railwayStationSP.selectedItemPosition
        val railwayStationNumber =
            railwayStations.getJSONObject(railwayStationNumberPos).getString("id").toString()

        val districtNumberPos = districtSP.selectedItemPosition
        val districtNumber =
            districts.getJSONObject(districtNumberPos).getString("id").toString()

        val contactCategoryNumberPos = contactsCategorySP.selectedItemPosition
        val contactCategoryNumber =
            contactCategories.getJSONObject(contactCategoryNumberPos).getString("id").toString()

        if (name.isEmpty()) {
            Helper.showToast(this, "Name is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if (contactNumber.isEmpty()) {
            Helper.showToast(this, "Contact number is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if (remarks.isEmpty()) {
            Helper.showToast(this, "Remarks is mandatory", Toast.LENGTH_SHORT)
            return null
        }

        val formData = JSONObject()
        formData.put("district", districtNumber)
        formData.put("utc_timestamp", utcTime)
        formData.put("police_station", policeStationNumber)
        formData.put("railway_station", railwayStationNumber)
        formData.put("contacts_category", contactCategoryNumber)
        formData.put("name", name)
        formData.put("contact_number", contactNumber)
        formData.put("remarks", remarks)
        locationUtil.exportLocation(formData)
        return formData
    }

    private fun sendForm(formData: JSONObject) {
        try {
            val clientNT = OkHttpClient().newBuilder().build()
            val token = Helper.getData(this, Scope.TOKEN)
            val request = API.postRequest(token!!, API.EMERGENCY_CONTACTS, formData, file = null, fileName = null
                )
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM) {
                    removeEmergencyContacts()
                }
                Helper.showToast(this, "Emergency contacts saved", Toast.LENGTH_SHORT)
                finish()
            } else {
                val apiResponse  = response.body!!.string()
                val errorMessage = Helper.getError(apiResponse)
                Helper.showToast(this, errorMessage, Toast.LENGTH_LONG)
            }
        } catch (e: Exception) {
            Log.d("RailMaithri", e.stackTraceToString())
            saveForm(formData)
        }
    }

    private fun saveForm(formData: JSONObject) {
        if (mode == Scope.MODE_UPDATE_FORM) {
            removeEmergencyContacts()
        }
        val savedStr  = Helper.getObject(this, Scope.EMERGENCY_CONTACTS)!!
        val savedData = JSONObject(savedStr)
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.EMERGENCY_CONTACTS, savedData.toString())

        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
    }

    private fun removeEmergencyContacts() {
        val savedStr  = Helper.getObject(this, Scope.EMERGENCY_CONTACTS)!!
        val savedData = JSONObject(savedStr)
        savedData.remove(utcTime)
        Helper.saveData(this, Scope.EMERGENCY_CONTACTS, savedData.toString())
    }
}
