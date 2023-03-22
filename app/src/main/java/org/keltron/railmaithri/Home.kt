package org.keltron.railmaithri

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class Home : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)
        supportActionBar!!.hide()

        val addIncidentBT    = findViewById<Button>(R.id.add_incident_report)
        val searchIncidentBT = findViewById<Button>(R.id.search_incident_report)
        val savedIncidentBT  = findViewById<Button>(R.id.saved_incident_report)
        addIncidentBT.setOnClickListener {
            val intent = Intent(this, IncidentReport::class.java)
            intent.putExtra("mode", Scope.MODE_NEW_FORM)
            startActivity(intent)
        }
        savedIncidentBT.setOnClickListener {
            val intent = Intent(this, SavedData::class.java)
            intent.putExtra("scope", Scope.INCIDENT_REPORT)
            startActivity(intent)
        }
        searchIncidentBT.setOnClickListener {
            val intent = Intent(this, SearchData::class.java)
            intent.putExtra("scope", Scope.INCIDENT_REPORT)
            startActivity(intent)
        }
    }
}