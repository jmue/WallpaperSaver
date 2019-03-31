package com.github.jmue.wallpapersaver

import android.Manifest
import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private var mLayout: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mLayout = findViewById(R.id.main_layout)

        save_button.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "image/png"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, "wallpaper")
            }
            startActivityForResult(intent, WRITE_REQUEST_CODE)
        }

        showWallpaperPreview()
    }

    override fun onResume() {
        super.onResume()
        showWallpaperPreview()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                         resultData: Intent?) {
        if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri -> SaveWallpaperTask(uri).execute() }
        }
    }

    private fun showWallpaperPreview() {
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestExternalStoragePermission()
        } else {
            updateWallpaperPreview()
        }
    }

    private fun updateWallpaperPreview() {
        wallpaper_image.setImageDrawable(WallpaperManager.getInstance(this).drawable)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    updateWallpaperPreview()
                } else {
                    // permission denied, boo!
                }
            }
        }
    }

    private fun requestExternalStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Snackbar.make(mLayout!!, R.string.external_storage_access_required,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok) {
                ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
            }.show()

        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
        }
    }

    private inner class SaveWallpaperTask internal constructor(private val uri: Uri) : AsyncTask<Void, Void, Void>() {

        override fun doInBackground(vararg voids: Void): Void? {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val wallpaper = drawableToBitmap(WallpaperManager.getInstance(this@MainActivity).drawable)
                    wallpaper.compress(Bitmap.CompressFormat.PNG, 0, outputStream)
                }
            } catch (ignored: IOException) {
            }

            return null
        }

    }

    companion object {

        private const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 23
        private const val WRITE_REQUEST_CODE = 42

        private fun drawableToBitmap(drawable: Drawable): Bitmap {
            if (drawable is BitmapDrawable) {
                return drawable.bitmap
            }
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
    }
}
