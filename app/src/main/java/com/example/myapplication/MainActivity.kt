package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.databinding.ItemImageBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.Locale
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

class MainActivity : AppCompatActivity() {

    // 1. 启用 ViewBinding
    private lateinit var binding: ActivityMainBinding

    // 2. 图片数据列表
    private val imageUris = mutableListOf<Uri>()

    // 3. 图片列表的适配器
    private lateinit var imageAdapter: ImageAdapter

    // 4. 位置信息
    private var currentLocation: Location? = null

    // 5. Mock 数据
    private val hotTopics = listOf("#搞笑日常", "#美食探店", "#旅行vlog", "#萌宠", "#学习打卡")
    private val mockUsers = listOf("张三", "李四", "王五", "赵六")

    // --- Launcher 定义区域 ---

    // 用于选择多张图片的Launcher
    private val pickImagesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
            val remainingSlots = 9 - imageUris.size
            val urisToAdd = uris.take(remainingSlots)
            imageUris.addAll(urisToAdd)
            imageAdapter.notifyDataSetChanged()
            if (uris.size > remainingSlots) {
                Toast.makeText(this, "最多只能选择9张图片", Toast.LENGTH_SHORT).show()
            }
        }

    // 用于拍照的Launcher
    private lateinit var photoUri: Uri // 用于存储拍照后图片的Uri
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && ::photoUri.isInitialized) {
                imageUris.add(photoUri)
                imageAdapter.notifyDataSetChanged()
            } else {
                Toast.makeText(this, "拍照失败", Toast.LENGTH_SHORT).show()
            }
        }

    // 用于请求位置权限的Launcher
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "位置权限被拒绝", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        initTextEditor()
        initFeatureButtons()
        initLocation()

        binding.buttonGenerateCaption.setOnClickListener {
            if (imageUris.isEmpty()) {
                Toast.makeText(this, "请先选择一张图片", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            generateCaptionWithAI(imageUris.first()) // 只分析第一张图
        }


        // 发布按钮的点击事件
        binding.buttonPublish.setOnClickListener {
            val caption = binding.editTextCaption.text.toString()
            if (imageUris.isEmpty()) {
                Toast.makeText(this, "请至少选择一张图片", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (caption.isBlank()) {
                Toast.makeText(this, "请添加一段描述", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "发布成功！", Toast.LENGTH_LONG).show()

            // 清空内容，准备下一次发布
            imageUris.clear()
            imageAdapter.notifyDataSetChanged()
            binding.editTextCaption.setText("")
            binding.textViewLocationInfo.visibility = View.GONE
        }
    }

    private fun initViews() {
        imageAdapter = ImageAdapter(imageUris,
            onDeleteClick = { position ->
                imageUris.removeAt(position)
                imageAdapter.notifyDataSetChanged()
                Toast.makeText(this, "已删除图片", Toast.LENGTH_SHORT).show()
            },
            onAddClick = {
                if (imageUris.size >= 9) {
                    Toast.makeText(this, "最多只能选择9张图片", Toast.LENGTH_SHORT).show()
                    return@ImageAdapter
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("添加图片")
                    .setItems(arrayOf("拍照", "从相册选择")) { _, which ->
                        when (which) {
                            0 -> openCamera() // 选择拍照
                            1 -> pickImagesLauncher.launch("image/*") // 选择相册
                        }
                    }
                    .show()
            }
        )
        binding.recyclerViewImages.adapter = imageAdapter
        binding.recyclerViewImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // 添加拖拽排序功能
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                // 不能拖动"添加"按钮
                if (viewHolder.adapterPosition == imageUris.size) return 0
                val dragFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                return makeMovementFlags(dragFlags, 0)
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                // 不能移动到"添加"按钮的位置
                if (target.adapterPosition == imageUris.size) return false
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                Collections.swap(imageUris, fromPosition, toPosition)
                imageAdapter.notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewImages)
    }

    private fun initTextEditor() {
        binding.editTextCaption.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                binding.textViewCharCount.text = "$length/500"
            }
        })
    }

    private fun initFeatureButtons() {
        binding.textViewAddTopic.setOnClickListener {
            showBottomSheet(hotTopics) { topic ->
                insertTextIntoEditText(topic)
            }
        }

        binding.textViewAddUser.setOnClickListener {
            showBottomSheet(mockUsers) { user ->
                insertTextIntoEditText("@$user ")
            }
        }
    }

    private fun insertTextIntoEditText(textToInsert: String) {
        val editText = binding.editTextCaption
        val start = editText.selectionStart
        val end = editText.selectionEnd
        val editable = editText.text
        editable.replace(start, end, textToInsert)
    }

    private fun showBottomSheet(items: List<String>, onItemSelected: (String) -> Unit) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_bottom_sheet_list, null)
        val listView = sheetView.findViewById<android.widget.ListView>(R.id.bottomSheetListView)

        listView.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        listView.setOnItemClickListener { _, _, position, _ ->
            onItemSelected(items[position])
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.setContentView(sheetView)
        bottomSheetDialog.show()
    }

    // --- 相机和位置功能 ---
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // 用户授权了，再次调用打开相机
                launchCamera()
            } else {
                // 用户拒绝了权限
                Toast.makeText(this, "相机权限被拒绝，无法拍照", Toast.LENGTH_SHORT).show()
            }
        }

    private fun openCamera() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                // 已经有权限了，直接启动相机
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // 用户之前拒绝过，需要解释为什么需要这个权限
                AlertDialog.Builder(this)
                    .setTitle("需要相机权限")
                    .setMessage("为了拍摄照片，请允许使用相机权限。")
                    .setPositiveButton("授权") { _, _ ->
                        // 用户点击授权，再次发起请求
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            else -> {
                // 用户是第一次请求，直接请求权限
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        try {
            val photoFile = File.createTempFile("IMG_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES))
            photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(this, "无法启动相机: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val locationManager by lazy { getSystemService(LOCATION_SERVICE) as LocationManager }

    private fun initLocation() {
        binding.textViewAddLocation.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                    getCurrentLocation()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                    AlertDialog.Builder(this)
                        .setTitle("需要位置权限")
                        .setMessage("为了添加您的位置信息，请允许获取位置权限。")
                        .setPositiveButton("授权") { _, _ ->
                            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
                else -> {
                    locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                }
            }
        }
    }

    private fun getCurrentLocation() {
        // 为了演示效果，我们使用模拟位置
        val mockLocation = Location("mock").apply {
            latitude = 39.9042  // 北京纬度
            longitude = 116.4074 // 北京经度
        }
        updateLocationUI(mockLocation)

        // 真实设备获取位置的代码
        /*
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "请先开启GPS定位服务", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }
        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lastKnownLocation != null) {
            updateLocationUI(lastKnownLocation)
            return
        }
        locationManager.requestSingleUpdate(
            LocationManager.GPS_PROVIDER,
            object : LocationListener { override fun onLocationChanged(location: Location) { updateLocationUI(location) } },
            Looper.getMainLooper()
        )
        */
    }

    // 调用大模型
    private  val ZHIPU_API_KEY = "74d1739f047c44f9b7ada776030457e8.46Z4ZjwdkM8ASPGH"

    private fun generateCaptionWithAI(imageUri: Uri) {
        // 显示加载状态
        binding.buttonGenerateCaption.text = "AI思考中..."
        binding.buttonGenerateCaption.isEnabled = false

        // 在后台线程执行网络请求
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 将图片转换为Base64
                val inputStream = contentResolver.openInputStream(imageUri)
                val imageBytes = inputStream?.readBytes()
                val base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)

                // 构建请求体
                val jsonBody = """
                {
                  "model": "glm-4v",
                  "messages": [
                    {
                      "role": "user",
                      "content": [
                        {
                          "type": "text",
                          "text": "请为这张图片写一段适合发朋友圈的文案，风格要轻松、有趣，不超过80字。"
                        },
                        {
                          "type": "image_url",
                          "image_url": {
                            "url": "data:image/jpeg;base64,${base64Image.trim()}"
                          }
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()

                // 发送请求
                Log.d("AI_REQUEST", "Request body: $jsonBody")
                val client = okhttp3.OkHttpClient()


                val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())


                val request = okhttp3.Request.Builder()
                    .url("https://open.bigmodel.cn/api/paas/v4/chat/completions")
                    .addHeader("Authorization", "Bearer $ZHIPU_API_KEY")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val responseBody = response.body?.string()
                    Log.d("AI_RESPONSE", "Raw response: $responseBody")
                    // 解析JSON获取AI生成的文案
                    val jsonObject = org.json.JSONObject(responseBody ?: "")
                    val choices = jsonObject.getJSONArray("choices")
                    val firstChoice = choices.getJSONObject(0)
                    val message = firstChoice.getJSONObject("message")
                    val content = message.getString("content")

                    // 切换回主线程更新UI
                    withContext(Dispatchers.Main) {
                        binding.editTextCaption.setText(content.trim())
                        binding.buttonGenerateCaption.text = "AI生成"
                        binding.buttonGenerateCaption.isEnabled = true
                        Toast.makeText(this@MainActivity, "文案生成成功！", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 切换回主线程更新UI
                withContext(Dispatchers.Main) {
                    binding.buttonGenerateCaption.text = "AI生成"
                    binding.buttonGenerateCaption.isEnabled = true
                    Toast.makeText(this@MainActivity, "生成失败：" + e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 将经纬度转换为城市信息
    private fun updateLocationUI(location: Location) {
        currentLocation = location
        binding.textViewLocationInfo.text = "正在解析位置..."
        binding.textViewLocationInfo.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                withContext(Dispatchers.Main) {
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        // 优先显示城市，没有则显示省份
                        val locationText = address.locality ?: address.adminArea ?: "未知位置"
                        binding.textViewLocationInfo.text = locationText
                    } else {
                        binding.textViewLocationInfo.text = "位置解析失败"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.textViewLocationInfo.text = "无法获取位置信息"
                }
            }
        }
    }
}

// --- ImageAdapter ---
class ImageAdapter(
    private val uris: MutableList<Uri>,
    private val onDeleteClick: (Int) -> Unit,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        if (position == uris.size) {
            holder.binding.imageViewItem.visibility = View.GONE
            holder.binding.textViewAddButton.visibility = View.VISIBLE
            holder.binding.imageViewDelete.visibility = View.GONE
            holder.binding.root.setOnClickListener { onAddClick() }
        } else {
            holder.binding.imageViewItem.visibility = View.VISIBLE
            holder.binding.textViewAddButton.visibility = View.GONE
            holder.binding.imageViewDelete.visibility = View.VISIBLE

            Glide.with(holder.itemView.context)
                .load(uris[position])
                .into(holder.binding.imageViewItem)

            holder.binding.imageViewDelete.setOnClickListener { onDeleteClick(position) }
            holder.binding.root.setOnClickListener {}
        }
    }

    override fun getItemCount(): Int = if (uris.size >= 9) uris.size else uris.size + 1
}
