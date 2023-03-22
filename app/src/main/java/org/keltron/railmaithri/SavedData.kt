package org.keltron.railmaithri

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import okhttp3.OkHttpClient
import org.json.JSONObject

class SavedData : AppCompatActivity() {
    private lateinit var clientNT:    OkHttpClient
    private lateinit var progressPB:  ProgressBar
    private lateinit var savedDataLY: LinearLayout
    private lateinit var formNameTV:  TextView
    private lateinit var syncBT:      Button
    private lateinit var scope:       String

    override fun onResume() {
        super.onResume()
        savedDataLY.removeAllViews()
        val savedData = JSONObject(Helper.getObject(this, scope)!!)
        val uuids     = savedData.keys()
        while (uuids.hasNext()) {
            val uuid       = uuids.next()
            val savedDatum = savedData.getJSONObject(uuid)
            injectDatum(uuid, savedDatum)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.saved_data)
        supportActionBar!!.hide()

        clientNT    = OkHttpClient().newBuilder().build()
        savedDataLY = findViewById(R.id.ly_saved_data)
        formNameTV  = findViewById(R.id.form_name)
        syncBT      = findViewById(R.id.sync)
        progressPB  = findViewById(R.id.progress_bar)
        scope       = intent.getStringExtra("scope")!!

        progressPB.visibility = View.GONE
        formNameTV.text       = Helper.prettify(scope)
    }

    private fun injectDatum(uuid: String, savedDatum: JSONObject){
        Log.e("RailMaithri", savedDatum.toString())
        val button  = Button(this)
        button.text = uuid
        savedDataLY.addView(button)

        button.setOnClickListener {
            var intent: Intent? = null
            if(scope == Scope.INCIDENT_REPORT){
                intent = Intent(this, IncidentReport::class.java)
            }
            intent!!.putExtra("mode", Scope.MODE_UPDATE_FORM)
            intent.putExtra("saved_data", savedDatum.toString())
            startActivity(intent)
        }
    }
}