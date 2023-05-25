package com.example.segundoapp

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.segundoapp.databinding.ActivityMainBinding
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.maps.android.heatmaps.HeatmapTileProvider





class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val REQUEST_CODE = 123 // Valor
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: DatabaseReference
    private lateinit var googleMap: GoogleMap

    val db = FirebaseFirestore.getInstance()
    var handler = Handler(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clearCache(applicationContext)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permissão já foi concedida
            // Coloque aqui o código para usar a localização
        } else {
            // Permissão ainda não foi concedida, solicite-a ao usuário
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE
            )
        }

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapViewContainer) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        if (mapFragment == null) {
            val newMapFragment = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(R.id.mapViewContainer, newMapFragment)
                .commit()
            newMapFragment.getMapAsync(this)
        } else {
            mapFragment.getMapAsync(this)
        }


        val runnable = Runnable {
            // Código a ser executado na thread principal
            consulta()
        }
        // Executa o código na thread principal após um atraso de 10 segundos para nao lotar o firebase de requisições e estourar o limite
        handler.postDelayed(runnable, 10000)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        val zoomlevel = 15.0f
        val PUCC = LatLng(-22.834788, -47.049731)
      googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(PUCC,zoomlevel))
    }

    fun consulta() {
        val collectionRef = db.collection("IntegrasData")
        val dadosList = mutableListOf<IntegrasData>()
        val latLngList = mutableListOf<LatLng>()

        collectionRef.get()
            .addOnSuccessListener { querySnapshot ->
                for (documentSnapshot in querySnapshot) {
                    val nivelRuido = documentSnapshot.getDouble("SPL")?.toFloat()
                    val dataFirestore = documentSnapshot.getDate("data")
                    val latitude = documentSnapshot.getDouble("Latitude")
                    val longitude = documentSnapshot.getDouble("Longitude")
                    // Acesso aos dados de cada documento retornado
                    val data = documentSnapshot.data
                    // Lógica para manipular os dados
                    if (nivelRuido != null && latitude != null && longitude != null) {
                        val dados = IntegrasData(nivelRuido, latitude, longitude)
                        val latLng = LatLng(latitude, longitude)
                        latLngList.add(latLng)
                        dadosList.add(dados)
                    }
                }
                for (dados in dadosList) {
                    Log.d(
                        TAG, "Nivel de ruido: ${dados.nivelRuido}\n" +
                                "Latitude: ${dados.latitude}\n" +
                                "Longitude: ${dados.longitude}"
                    )
                }
                val heatMap = HeatmapTileProvider.Builder()
                    .data(latLngList)
                    .build()

                googleMap.addTileOverlay(TileOverlayOptions().tileProvider(heatMap))

            }

            .addOnFailureListener { exception ->
                "Erro ao tentar consultar o banco de dados na coleção ${collectionRef}: ${exception}"
                // Lógica para lidar com erros
            }

    }
    fun clearCache(context: Context) {
        context.cacheDir?.let { cacheDir ->
            if (cacheDir.exists()) {
                val files = cacheDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        file.delete()
                    }
                }
            }
        }
    }
}


