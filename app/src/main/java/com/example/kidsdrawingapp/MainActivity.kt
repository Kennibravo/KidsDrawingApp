package com.example.kidsdrawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.example.kidsdrawingapp.databinding.ActivityMainBinding
import com.example.kidsdrawingapp.databinding.DialogBrushSizeBinding
import kotlinx.coroutines.*
import top.defaults.colorpicker.ColorPickerPopup
import top.defaults.colorpicker.ColorPickerPopup.ColorPickerObserver
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private var scope = CoroutineScope(Dispatchers.Main)

    private lateinit var binding: ActivityMainBinding
    private lateinit var brushDialogBinding: DialogBrushSizeBinding
    private var mImageButtonCurrentPaint: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        brushDialogBinding = DialogBrushSizeBinding.inflate(layoutInflater)

        binding.drawingView.setSizeForBrush(20.toFloat())

        mImageButtonCurrentPaint = binding.llPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))

        binding.ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        binding.ibGallery.setOnClickListener {
            if (isReadStorageAllowed()) {
                val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotoIntent, GALLERY)
            } else {
                requestStoragePermission()
            }

        }

        binding.ibUndo.setOnClickListener {
            binding.drawingView.onClickUndo()
        }

        binding.ibSave.setOnClickListener {
            if (isReadStorageAllowed()) {
                scope.launch {
                    val t: Boolean = bitmapTask(getBitmapFromView(binding.flDrawingViewContainer))
                    println("Good to go!")
                    if(t) Toast.makeText(this@MainActivity, "Downloaded", Toast.LENGTH_LONG).show()

                }
            }
        }
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                try {
                    binding.ivBackground.visibility = View.VISIBLE
                    binding.ivBackground.setImageURI(data!!.data)
                } catch (e: Exception) {
                    Toast.makeText(this, "You cannot use this image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)

        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size")

        val smallBtn: ImageButton? = brushDialog.findViewById(R.id.ib_small_brush)
        val mediumBtn: ImageButton? = brushDialog.findViewById(R.id.ib_medium_brush)
        val largeBtn: ImageButton? = brushDialog.findViewById(R.id.ib_large_brush)

        smallBtn?.setOnClickListener {
            Log.i("Test sth", "Small")
            binding.drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        mediumBtn?.setOnClickListener {
            Log.i("Test sth", "Small")
            binding.drawingView.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        largeBtn?.setOnClickListener {
            Log.i("Test sth", "Small")
            binding.drawingView.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view: View) {
        if (view != mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton

            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            binding.drawingView.setColor(view.tag.toString())

            mImageButtonCurrentPaint!!.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint = view
        }
    }

    fun showColorPicker(view: View) {
        ColorPickerPopup.Builder(this)
                .initialColor(Color.RED) // Set initial color
                .enableBrightness(true) // Enable brightness slider or not
                .enableAlpha(true) // Enable alpha slider or not
                .okTitle("Choose")
                .cancelTitle("Cancel")
                .showIndicator(true)
                .showValue(true)
                .build()
                .show(view, object : ColorPickerObserver() {
                    override fun onColorPicked(color: Int) {
                        binding.drawingView.setColor(color.toString(), "int")

                        mImageButtonCurrentPaint = view as ImageButton
                    }

                    fun onColor(color: Int, fromUser: Boolean) {}
                })
    }

    private fun requestStoragePermission() {
        var permission: Array<String> = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        permission.toString())) {
            Toast.makeText(this, "You need permission to add a background!", Toast.LENGTH_LONG).show()
        }

        ActivityCompat.requestPermissions(this, permission, STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted, you can now read Storage files", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Oops, you denied the permission!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background

        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }

    private suspend fun bitmapTask(mBitmap: Bitmap): Boolean{
        return withContext(Dispatchers.IO){
            downloadBitmap(mBitmap)
        }
    }

    private suspend fun downloadBitmap(mBitmap: Bitmap): Boolean {
        return try {
            val bytes = ByteArrayOutputStream()
            mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
            val f = File(externalCacheDir!!.absoluteFile.toString()
                    + File.separator + "KidDrawingApp_"
                    + System.currentTimeMillis() / 1000 + ".png")

            val fos = FileOutputStream(f)
            fos.write(bytes.toByteArray())
            fos.close()

            delay(7000)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}