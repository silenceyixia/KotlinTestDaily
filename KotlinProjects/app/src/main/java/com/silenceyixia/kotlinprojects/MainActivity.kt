package com.silenceyixia.kotlinprojects

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.CameraUpdateFactory
import com.amap.api.maps2d.MapView
import com.amap.api.maps2d.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.silenceyixia.kotlinprojects.model.TitleModel
import java.io.*


class MainActivity : AppCompatActivity(), AMap.OnMyLocationChangeListener {

    val TAG: String = "MainActivity"

    private var mapView: MapView? = null
    private var map: AMap? = null
    private var locationClient: AMapLocationClient? = null
    private var inited = false
    private var loaded = false
    private var mTileOverlay: TileOverlay? = null
    private var titleModel: TitleModel? = null

    private val mPermissions = arrayOf(Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission(mPermissions)
        mapView = findViewById(R.id.map_view)
        try {
            mapView!!.onCreate(savedInstanceState)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
        initData()
        initMap()

        // Example of a call to a native method
        // sample_text.text = stringFromJNI()

        Log.e(TAG, "" + android.os.Environment.getExternalStorageDirectory())
        Log.e(TAG, "" + android.os.Environment.getDataDirectory())
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

    private fun checkPermission(needPermissions: Array<String>): Boolean {
        var needPermission = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val needRequestPermissionList = findDeniedPermissions(needPermissions)
            if (!needRequestPermissionList.isEmpty()) {
                requestPermissions(needRequestPermissionList.toTypedArray(), 1024)
                needPermission = true
            }
        }
        return needPermission
    }

    private fun findDeniedPermissions(permissions: Array<String>): List<String> {
        return permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED || ActivityCompat.shouldShowRequestPermissionRationale(this, it)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1024) {
            if (!verifyPermissions(grantResults)) {
                showMissingPermissionDialog()
                return
            }
            onRequestPermission()
        } else {
            finish()
        }
    }

    private fun showMissingPermissionDialog() {

    }

    private fun onRequestPermission() {

    }

    private fun verifyPermissions(grantResults: IntArray): Boolean {
        return grantResults.none { it != PackageManager.PERMISSION_GRANTED }
    }

    private fun initData() {
        val jsonStr = getJson("scenic.json", this)
        val type = object : TypeToken<TitleModel>() {}.type
        titleModel = Gson().fromJson<TitleModel>(jsonStr, type)

//        translateImgs()
    }

    private fun translateImgs() {
        val assets = resources.assets
        val list = assets.list("")
        list.forEach {
            try {
                writeFile(it, assets.open(it))
            } catch (e: IOException) {
                e.toString()
            }
        }
    }

    private fun writeFile(name: String, inputString: InputStream?) {
        val absoluteFile = cacheDir.absolutePath
//        val sdPath = android.os.Environment.getExternalStorageDirectory().absolutePath
        try {
            val file = File(absoluteFile + "/" + name)
            if (file.exists()) return
            val fos = FileOutputStream(absoluteFile + "/" + name)
            var length: Int
            val byte = ByteArray(1024)
            while (true) {
                length = inputString?.read(byte)!!
                if (length == -1) break
                fos.write(byte, 0, length)
                fos.flush()
            }
            fos.close()
        } catch (e: IOException) {
            e.toString()
        } finally {
            inputString?.close()
        }
    }

    private fun getJson(fileName: String, context: Context): String {
        //将json数据变成字符串
        val stringBuilder = StringBuilder()
        try {
            val assets = resources.assets
            val bufferedReader = BufferedReader(InputStreamReader(assets.open(fileName)))
            var line: String?
            while (true) {
                line = bufferedReader.readLine() ?: break
                stringBuilder.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return stringBuilder.toString()
    }

    private fun initMap() {
        if (map == null) {
            map = mapView!!.map
            val uiSettings = map!!.uiSettings
            val mLocationStyle = MyLocationStyle()
            mLocationStyle.interval(5000)
            mLocationStyle.showMyLocation(true)
            mLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER)
            map!!.setOnMyLocationChangeListener(this)
            map!!.setMyLocationStyle(mLocationStyle)
            map!!.isMyLocationEnabled = true

        }
    }

    override fun onMyLocationChange(p0: Location?) {
        if (null != p0) {
            if (!inited) {
                if (p0.latitude != 0.0 && p0.longitude != 0.0) {
                    inited = true
                }
                val latLng = LatLng(p0.latitude, p0.longitude)
                map!!.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition(latLng, 17f, 0f, 0f)), 500, null)
            } else {
                if (inited && !loaded) {
                    map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(titleModel?.root?.latitude!!, titleModel?.root?.longitude!!), (titleModel?.root?.min_zoom)!!.toFloat()))
                    loaded = true
                }
//                useOfflineTile()
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        mapView!!.onResume()
        useOfflineTile()
    }

    public override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView!!.onSaveInstanceState(outState)
    }

    public override fun onDestroy() {
        super.onDestroy()
        mapView!!.onDestroy()
        if (null != locationClient) {
//            locationClient!!.unRegisterLocationListener(this)
            locationClient!!.stopLocation()
            locationClient!!.onDestroy()
        }
    }

    /**
     * 加载离线瓦片数据
     */
    private fun useOfflineTile() {
//        val url = cacheDir.absolutePath + "/%d_%d_%d.png"
        val url = cacheDir.absolutePath
        Log.e(TAG, "URL_PATH=" + url)
        val tileOverlayOptions = TileOverlayOptions().tileProvider(LocalTileProvider(url))
        LocalTileProvider(url).apply {  }
        tileOverlayOptions.diskCacheEnabled(true)
//                .diskCacheDir("/storage/emulated/0/amap/tilecache")
                .diskCacheDir(cacheDir.absolutePath)
                .diskCacheSize(100000)
                .memoryCacheEnabled(true)
                .memCacheSize(100000)
                .zIndex(-9999f)
        mTileOverlay = map!!.addTileOverlay(tileOverlayOptions)
    }

    class LocalTileProvider(private var tilePath: String) : TileProvider {

        companion object {

            private val TILE_WIDTH = 256

            private val TILE_HEIGHT = 256

            val BUFFER_SIZE = 16 * 1024
        }

        override fun getTileWidth(): Int {
            return TILE_WIDTH
        }

        override fun getTileHeight(): Int {
            return TILE_HEIGHT
        }

        override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
            val byteArray = readTileImage(x, y, zoom) ?: return null
            return Tile(TILE_WIDTH, TILE_HEIGHT, byteArray)
        }

        private fun readTileImage(x: Int, y: Int, zoom: Int): ByteArray? {
            var ins: FileInputStream? = null
            var buffer: ByteArrayOutputStream? = null
            val f = File(getTileFilename(x, y, zoom))
            if (f.exists()) {
                try {
                    buffer = ByteArrayOutputStream()
                    ins = FileInputStream(f)
                    var nRead: Int
                    val data = ByteArray(BUFFER_SIZE)
                    while (true) {
                        nRead = ins.read(data)
                        if (nRead == -1) {
                            break
                        }
                        buffer.write(data, 0, nRead)
                    }
                    buffer.flush()
                    return buffer.toByteArray()
                } catch (e: IOException) {
                    e.printStackTrace()
                    return null
                } catch (e: OutOfMemoryError) {
                    e.printStackTrace()
                    return null
                } finally {
                    ins?.close()
                    buffer?.close()
                }
            } else {
                return null
            }
        }

        private fun getTileFilename(x: Int, y: Int, zoom: Int): String {
                return tilePath + "/" + x + "_" + y + "_" + zoom + ".png"
        }
    }
}