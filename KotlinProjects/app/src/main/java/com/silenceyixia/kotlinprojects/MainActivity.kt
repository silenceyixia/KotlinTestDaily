package com.silenceyixia.kotlinprojects

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView

class MainActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    private var map: AMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.map_view)
        mapView!!.onCreate(savedInstanceState)
        map = mapView!!.map

        // Example of a call to a native method
        // sample_text.text = stringFromJNI()
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
