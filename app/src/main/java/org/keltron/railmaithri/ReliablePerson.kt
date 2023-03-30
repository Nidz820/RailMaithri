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

class ReliablePerson: AppCompatActivity() {

    private lateinit var progressPB:      ProgressBar
    private lateinit var saveBT:          Button
    private lateinit var policeStationSP: Spinner
    private lateinit var descriptionET:   EditText
    private lateinit var nameET:          EditText
    private lateinit var mobileNumberET:  EditText
    private lateinit var placeET:         EditText

    private lateinit var policeStationAP: ArrayAdapter<String>

    private lateinit var mode:            String
    private lateinit var policeStations:  JSONArray
    private lateinit var utcTime:         String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reliable_persons)
        supportActionBar!!.hide()

        utcTime         = Helper.getUTC()
        mode            = intent.getStringExtra("mode")!!
        progressPB      = findViewById(R.id.progress_bar)
        saveBT          = findViewById(R.id.save)
        policeStationSP = findViewById(R.id.police_station)
        nameET          = findViewById(R.id.name)
        descriptionET   = findViewById(R.id.description)
        mobileNumberET  = findViewById(R.id.contact_number)
        placeET         = findViewById(R.id.place)

        policeStations  = JSONArray(Helper.getData(this, Scope.POLICE_STATIONS_LIST)!!)
        policeStationAP = Helper.makeArrayAdapter(policeStations, this)
        policeStationSP.adapter = policeStationAP

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
        descriptionET.setText(data.getString("description"))
        nameET.setText(data.getString("name"))
        mobileNumberET.setText(data.getString("mobile_number"))
        placeET.setText(data.getString("place"))

        val policeStationNumber = data.getInt("police_station")
        val policeStationName = Helper.getName(policeStations, policeStationNumber)
        val policeStationNumberPos = policeStationAP.getPosition(policeStationName)
        policeStationSP.setSelection(policeStationNumberPos)
    }

    private fun validateInput(): JSONObject? {
        val name         = nameET.text.toString()
        val mobileNumber = mobileNumberET.text.toString()
        val place        = placeET.text.toString()
        val description  = descriptionET.text.toString()

        val policeStationNumberPos = policeStationSP.selectedItemPosition
        val policeStationNumber =
            policeStations.getJSONObject(policeStationNumberPos).getString("id").toString()

        if (name.isEmpty()) {
            Helper.showToast(this, "Name is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if (mobileNumber.isEmpty()) {
            Helper.showToast(this, "Mobile number is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if (place.isEmpty()) {
            Helper.showToast(this, "Place is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if (description.isEmpty()) {
            Helper.showToast(this, "Description is mandatory", Toast.LENGTH_SHORT)
            return null
        }

        val formData = JSONObject()
        formData.put("description", description)
        formData.put("utc_timestamp", utcTime)
        formData.put("name", name)
        formData.put("mobile_number", mobileNumber)
        formData.put("police_station", policeStationNumber)
        formData.put("place", place)

        return formData
    }

    private fun sendForm(formData: JSONObject) {
        try {
            val clientNT = OkHttpClient().newBuilder().build()
            val token    = Helper.getData(this, Scope.TOKEN)
            val request  =
                API.postRequest(token!!, API.RELIABLE_PERSON, formData, file = null, fileName = null
                )
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM) {
                    removeReliablePerson()
                }
                Helper.showToast(this, "Reliable person saved", Toast.LENGTH_SHORT)
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
            removeReliablePerson()
        }
        val savedStr  = Helper.getObject(this, Scope.RELIABLE_PERSON)!!
        val savedData = JSONObject(savedStr)
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.RELIABLE_PERSON, savedData.toString())

        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
    }

    private fun removeReliablePerson() {
        val savedStr  = Helper.getObject(this, Scope.RELIABLE_PERSON)!!
        val savedData = JSONObject(savedStr)
        savedData.remove(utcTime)
        Helper.saveData(this, Scope.RELIABLE_PERSON, savedData.toString())
    }
}