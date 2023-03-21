package org.keltron.railmaithri

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject


class Home : AppCompatActivity() {
    var incidentReport: String? = null

    override fun onResume() {
        super.onResume()
        incidentReport = null
        try{
            val incidentReportsString   = Helper.getData(this, Scope.INCIDENT_REPORT)
            val incidentReports         = JSONObject(incidentReportsString)
            val uuids                   = incidentReports.keys()
            incidentReport              = incidentReports.get(uuids.next()).toString()
        }catch (_: Exception){}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)
        supportActionBar!!.hide()

        val incidentReportBT = findViewById<Button>(R.id.incident_report)
        incidentReportBT.setOnClickListener {
            val intent = Intent(this, IncidentReport::class.java)
            if(incidentReport != null){
                intent.putExtra("mode", Scope.MODE_UPDATE_FORM)
                intent.putExtra("saved_data", incidentReport)
            } else {
                intent.putExtra("mode", Scope.MODE_NEW_FORM)
            }
            startActivity(intent)
        }
    }
}