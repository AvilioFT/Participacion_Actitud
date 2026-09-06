package com.example.entregable01actitud

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        findViewById<Button>(R.id.btnUsgs).setOnClickListener {
            abrirConsulta("USGS")
        }

        findViewById<Button>(R.id.btnNasa).setOnClickListener {
            abrirConsulta("NASA")
        }

        findViewById<Button>(R.id.btnGeoref).setOnClickListener {
            abrirConsulta("GEOREF")
        }

        findViewById<Button>(R.id.btnTcambio).setOnClickListener {
            abrirConsulta("TCAMBIO")
        }
    }

    private fun abrirConsulta(api: String) {
        val intent = Intent(this, ConsultaActivity::class.java)
        intent.putExtra("API_SELECCIONADA", api)
        startActivity(intent)
    }
}