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

class RailVolunteer: AppCompatActivity() {

    private lateinit var progressPB:                ProgressBar
    private lateinit var saveBT:                    Button
    private lateinit var fileUtil:                  FileUtil
    private lateinit var railVolunteerCategorySP:   Spinner
    private lateinit var railwayStationSP:          Spinner
    private lateinit var genderTypeSP:              Spinner
    private lateinit var nameET:                    EditText
    private lateinit var ageET:                     EditText
    private lateinit var mobileNumberET:            EditText
    private lateinit var emailET:                   EditText

    private lateinit var railVolunteerCategoryAP:   ArrayAdapter<String>
    private lateinit var railwayStationAP:          ArrayAdapter<String>
    private lateinit var genderTypeAP:              ArrayAdapter<String>

    private lateinit var mode:                      String
    private lateinit var railVolunteerCategories:   JSONArray
    private lateinit var railwayStations:           JSONArray
    private lateinit var genderTypes:               JSONArray
    private lateinit var utcTime:                   String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.rail_volunteers)
        supportActionBar!!.hide()
        utcTime                 = Helper.getUTC()
        mode                    = intent.getStringExtra("mode")!!
        fileUtil                = FileUtil(this, findViewById(R.id.ly_file), utcTime)
        progressPB              = findViewById(R.id.progress_bar)
        saveBT                  = findViewById(R.id.save)
        railVolunteerCategorySP = findViewById(R.id.rail_volunteer_category)
        railwayStationSP        = findViewById(R.id.railway_station)
        genderTypeSP            = findViewById(R.id.gender)
        nameET                  = findViewById(R.id.name)
        ageET                   = findViewById(R.id.age)
        mobileNumberET          = findViewById(R.id.mobile_number)
        emailET                 = findViewById(R.id.email)

        railVolunteerCategories = JSONArray(Helper.getData(this, Scope.RAIL_VOLUNTEER_TYPES)!!)
        railVolunteerCategoryAP = Helper.makeArrayAdapter(railVolunteerCategories, this)
        railVolunteerCategorySP.adapter = railVolunteerCategoryAP

        genderTypes  = JSONArray(Helper.getData(this, Scope.GENDER_TYPES)!!)
        genderTypeAP = Helper.makeArrayAdapter(genderTypes, this)
        genderTypeSP.adapter = genderTypeAP

        railwayStations  = JSONArray(Helper.getData(this, Scope.RAILWAY_STATIONS_LIST)!!)
        railwayStationAP = Helper.makeArrayAdapter(railwayStations, this)
        railwayStationSP.adapter = railwayStationAP

        progressPB.visibility = View.GONE
        saveBT.setOnClickListener {
            val inputData = validateInput()
            inputData?.let {
                progressPB.visibility = View.VISIBLE
                saveBT.isClickable = false
                CoroutineScope(Dispatchers.IO).launch {
                    val file = fileUtil.getFile()
                    val fileName = fileUtil.getFileName()
                    sendForm(inputData, file, fileName)
                    Handler(Looper.getMainLooper()).post {
                        saveBT.isClickable = true
                        progressPB.visibility = View.GONE
                    }
                }
            }
        }

        if (mode == Scope.MODE_VIEW_FORM) {
            saveBT.visibility = View.GONE
            fileUtil.disableUpdate()
        } else if (mode == Scope.MODE_UPDATE_FORM) {
            val savedData = intent.getStringExtra("saved_data")
            val formData  = JSONObject(savedData!!)
            utcTime = formData.getString("utc_timestamp")
            populateForm(formData)
        }
    }

    private fun populateForm(data: JSONObject) {
        nameET.setText(data.getString("name"))
        ageET.setText(data.getString("age"))
        mobileNumberET.setText(data.getString("mobile_number"))
        emailET.setText(data.getString("email"))

        if (data.has("file_name")) {
            val fileName = data.getString("file_name")
            fileUtil.loadFile(this, fileName)
        }
        val railVolunteerCategoryNumber    = data.getInt("rail_volunteer_category")
        val railVolunteerCategoryName      = Helper.getName(railVolunteerCategories, railVolunteerCategoryNumber)
        val railVolunteerCategoryNumberPos = railVolunteerCategoryAP.getPosition(railVolunteerCategoryName)
        railVolunteerCategorySP.setSelection(railVolunteerCategoryNumberPos)

        val railwayStationID   = data.getInt("railway_station")
        val railwayStationName = Helper.getName(railwayStations, railwayStationID)
        val railwayStationPos  = railwayStationAP.getPosition(railwayStationName)
        railwayStationSP.setSelection(railwayStationPos)

        val genderTypeID   = data.getInt("gender")
        val genderTypeName = Helper.getName(genderTypes, genderTypeID)
        val genderTypePos  = genderTypeAP.getPosition(genderTypeName)
        genderTypeSP.setSelection(genderTypePos)
    }

    private fun validateInput(): JSONObject? {
        val name         = nameET.text.toString()
        val age          = ageET.text.toString()
        val mobileNumber = mobileNumberET.text.toString()
        val email        = emailET.text.toString()

        val railVolunteerCategoryNumberPos = railVolunteerCategorySP.selectedItemPosition
        val railVolunteerCategoryNumber    = railVolunteerCategories.getJSONObject(railVolunteerCategoryNumberPos)
                                                .getString("id").toString()
        val railwayStationNumberPos = railwayStationSP.selectedItemPosition
        val railwayStationNumber    = railwayStations.getJSONObject(railwayStationNumberPos)
                                        .getString("id").toString()
        val genderTypeNumberPos = genderTypeSP.selectedItemPosition
        val genderTypeNumber    = genderTypes.getJSONObject(genderTypeNumberPos)
                                    .getString("id").toString()
        if (name.isEmpty()) {
            Helper.showToast(this, "Name is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if (age.isEmpty()) {
            Helper.showToast(this, "Age is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        val formData = JSONObject()
        formData.put("rail_volunteer_category", railVolunteerCategoryNumber)
        formData.put("data_from", "Citizen")
        formData.put("name", name)
        formData.put("age", age)
        formData.put("gender", genderTypeNumber)
        formData.put("nearest_railway_station", railwayStationNumber)
        formData.put("utc_timestamp", utcTime)
        formData.put("mobile_number", mobileNumber)
        formData.put("email", email)
        return formData
    }

    private fun sendForm(formData: JSONObject, file: ByteArray?, fileName: String?) {
        try {
            val clientNT = OkHttpClient().newBuilder().build()
            val token = Helper.getData(this, Scope.TOKEN)
            val request = API.postRequest(token!!, API.RAIL_VOLUNTEER, formData, file, fileName)
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM) {
                    removeRailVolunteer()
                }
                Helper.showToast(this, "Rail volunteer saved", Toast.LENGTH_SHORT)
                finish()
            } else {
                val apiResponse = response.body!!.string()
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
            removeRailVolunteer()
        }
        val savedStr  = Helper.getObject(this, Scope.RAIL_VOLUNTEER)!!
        val savedData = JSONObject(savedStr)
        if (fileUtil.haveFile()) {
            fileUtil.saveFile(this)
            formData.put("file_name", fileUtil.getFileName())
        }
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.RAIL_VOLUNTEER, savedData.toString())

        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
    }

    private fun removeRailVolunteer() {
        val savedStr  = Helper.getObject(this, Scope.RAIL_VOLUNTEER)!!
        val savedData = JSONObject(savedStr)
        savedData.remove(utcTime)
        Helper.saveData(this, Scope.RAIL_VOLUNTEER, savedData.toString())
        fileUtil.removeFile(this)
    }
}
