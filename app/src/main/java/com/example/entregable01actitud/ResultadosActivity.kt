package com.example.entregable01actitud

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ResultadosActivity : AppCompatActivity() {

    private lateinit var apiSeleccionada: String
    private lateinit var resultados: String
    private var resultadosCsv: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resultados)

        apiSeleccionada =
            intent.getStringExtra("API_SELECCIONADA") ?: "API"

        resultados =
            intent.getStringExtra("RESULTADOS") ?: "Sin resultados."

        resultadosCsv =
            intent.getStringExtra("RESULTADOS_CSV")

        val txtTituloResultados =
            findViewById<TextView>(R.id.txtTituloResultados)

        val txtResultados =
            findViewById<TextView>(R.id.txtResultados)

        val btnGenerarTexto =
            findViewById<Button>(R.id.btnGenerarTexto)

        txtTituloResultados.text = apiSeleccionada
        txtResultados.text = resultados

        btnGenerarTexto.setOnClickListener {

            val intent =
                Intent(this, TextosActivity::class.java)

            intent.putExtra(
                "API_SELECCIONADA",
                apiSeleccionada
            )

            intent.putExtra(
                "TEXTO_BASE",
                resultados
            )

            if (!resultadosCsv.isNullOrEmpty()) {
                intent.putExtra(
                    "TEXTO_CSV",
                    resultadosCsv
                )
            }

            startActivity(intent)
        }
    }
}