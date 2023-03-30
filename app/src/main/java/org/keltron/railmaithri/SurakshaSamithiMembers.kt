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

class SurakshaSamithiMembers: AppCompatActivity() {

    private lateinit var progressPB:        ProgressBar
    private lateinit var saveBT:            Button

    private lateinit var surakshaSamithiSP: Spinner

    private lateinit var nameET:            EditText
    private lateinit var addressET:         EditText
    private lateinit var mobileNumberET:    EditText
    private lateinit var emailET:           EditText

    private lateinit var surakshaSamithiAP: ArrayAdapter<String>

    private lateinit var mode:              String
    private lateinit var surakshaSamithies: JSONArray
    private lateinit var utcTime:           String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.suraksha_samithi_members)
        supportActionBar!!.hide()

        utcTime             = Helper.getUTC()
        mode                = intent.getStringExtra("mode")!!
        progressPB          = findViewById(R.id.progress_bar)
        saveBT              = findViewById(R.id.save)
        surakshaSamithiSP   = findViewById(R.id.suraksha_samithi)
        nameET              = findViewById(R.id.name)
        addressET           = findViewById(R.id.address)
        mobileNumberET      = findViewById(R.id.mobile_number)
        emailET             = findViewById(R.id.email)

        surakshaSamithies = JSONArray(Helper.getData(this, Scope.SURAKSHA_SAMITHI_LIST)!!)
        surakshaSamithiAP = Helper.makeArrayAdapter(surakshaSamithies, this)
        surakshaSamithiSP.adapter = surakshaSamithiAP

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
        addressET.setText(data.getString("address"))
        mobileNumberET.setText(data.getString("mobile_number"))

        val surakshaSamithiNumber = data.getInt("suraksha_samithi")
        val surakshaSamithiName =
            Helper.getName(surakshaSamithies, surakshaSamithiNumber)

        val surakshaSamithiNumberPos =
            surakshaSamithiAP.getPosition(surakshaSamithiName)
        surakshaSamithiSP.setSelection(surakshaSamithiNumberPos)
    }

    private fun validateInput(): JSONObject? {
        val name         = nameET.text.toString()
        val address      = addressET.text.toString()
        val mobileNumber = mobileNumberET.text.toString()

        val surakshaSamithiNumberPos = surakshaSamithiSP.selectedItemPosition
        val surakshaSamithiNumber =
            surakshaSamithies.getJSONObject(surakshaSamithiNumberPos).getString("id")
                .toString()

        if (name.isEmpty()) {
            Helper.showToast(this, "Name is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if (address.isEmpty()) {
            Helper.showToast(this, "Address is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if (mobileNumber.isEmpty()) {
            Helper.showToast(this, "Mobile number is mandatory", Toast.LENGTH_SHORT)
            return null
        }

        val formData = JSONObject()
        formData.put("suraksha_samithi", surakshaSamithiNumber)
        formData.put("name", name)
        formData.put("address", address)
        formData.put("utc_timestamp", utcTime)
        formData.put("mobile_number", mobileNumber)

        return formData
    }

    private fun sendForm(formData: JSONObject) {
        try {
            val clientNT = OkHttpClient().newBuilder().build()
            val token    = Helper.getData(this, Scope.TOKEN)
            val request  =
                API.postRequest(token!!, API.SURAKSHA_SAMITHI_MEMBERS, formData, file=null, fileName=null)
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM) {
                    removeSurakshaSamithiMembers()
                }
                Helper.showToast(this, "Suraksha samithi members saved", Toast.LENGTH_SHORT)
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
            removeSurakshaSamithiMembers()
        }
        val savedStr  = Helper.getObject(this, Scope.SURAKSHA_SAMITHI_MEMBERS)!!
        val savedData = JSONObject(savedStr)
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.SURAKSHA_SAMITHI_MEMBERS, savedData.toString())

        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
    }

    private fun removeSurakshaSamithiMembers() {
        val savedStr  = Helper.getObject(this, Scope.SURAKSHA_SAMITHI_MEMBERS)!!
        val savedData = JSONObject(savedStr)
        savedData.remove(utcTime)
        Helper.saveData(this, Scope.SURAKSHA_SAMITHI_MEMBERS, savedData.toString())
    }
}