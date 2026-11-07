
package com.lumisync

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val executor = Executors.newSingleThreadExecutor()
    private var deviceIp: String? = null
    private lateinit var status: TextView
    private lateinit var brightnessText: TextView
    private lateinit var brightnessBar: SeekBar

    private val bg = Color.rgb(12, 16, 29)
    private val panel = Color.rgb(25, 32, 51)
    private val accent = Color.rgb(111, 130, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 28)
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this)
        scroll.addView(root)

        val title = TextView(this).apply {
            text = "LUMISYNC"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 4)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "SMART LIGHTING CONTROL"
            textSize = 12f
            setTextColor(accent)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        root.addView(subtitle)

        status = TextView(this).apply {
            text = "● Cihaz bağlı değil\nGovee cihazını arayabilirsin."
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(18, 20, 18, 20)
            setBackgroundColor(panel)
        }
        root.addView(status, matchWidth())

        addSection(root, "CİHAZ BAĞLANTISI")

        addButton(root, "GOVEE CİHAZINI BUL") {
            discoverDevice()
        }

        addSection(root, "GÜÇ KONTROLÜ")

        val powerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        addButton(powerRow, "AÇ") {
            sendCommand("turn", JSONObject().put("value", 1))
        }

        addButton(powerRow, "KAPAT") {
            sendCommand("turn", JSONObject().put("value", 0))
        }

        root.addView(powerRow, matchWidth())

        addSection(root, "PARLAKLIK")

        brightnessText = TextView(this).apply {
            text = "Parlaklık: 100%"
            textSize = 15f
            setTextColor(Color.WHITE)
        }
        root.addView(brightnessText)

        brightnessBar = SeekBar(this).apply {
            max = 100
            progress = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    brightnessText.text = "Parlaklık: $progress%"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    sendCommand(
                        "brightness",
                        JSONObject().put("value", seekBar?.progress ?: 100)
                    )
                }
            })
        }
        root.addView(brightnessBar, matchWidth())

        addSection(root, "RENK SEÇİMİ")

        val colors = listOf(
            "Kırmızı" to Color.RED,
            "Yeşil" to Color.GREEN,
            "Mavi" to Color.BLUE,
            "Mor" to Color.rgb(160, 60, 255),
            "Turuncu" to Color.rgb(255, 130, 20),
            "Beyaz" to Color.WHITE
        )

        colors.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            pair.forEach { item ->
                addButton(row, item.first) {
                    sendColor(item.second)
                }
            }

            root.addView(row, matchWidth())
        }

        addSection(root, "SONRAKİ AŞAMA")

        val info = TextView(this).apply {
            text = "Ekran Senkronizasyonu\nSes Tepkisi\nBirleşik Mod\nSahne ve efektler"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(16, 16, 16, 16)
            setBackgroundColor(panel)
        }
        root.addView(info, matchWidth())

        setContentView(scroll)
    }

    private fun addSection(parent: LinearLayout, text: String) {
        val label = TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(accent)
            setPadding(0, 24, 0, 10)
        }
        parent.addView(label)
    }

    private fun addButton(parent: LinearLayout, text: String, action: () -> Unit) {
        val button = Button(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(accent)
            )
            setOnClickListener { action() }
        }

        val params = LinearLayout.LayoutParams(
            0,
            54,
            1f
        ).apply {
            setMargins(4, 5, 4, 5)
        }

        parent.addView(button, params)
    }

    private fun matchWidth(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun discoverDevice() {
        status.text = "● Govee cihazı aranıyor..."

        executor.execute {
            var foundIp: String? = null
            var responseText = ""

            try {
                DatagramSocket(4002).use { socket ->
                    socket.broadcast = true
                    socket.soTimeout = 5000

                    val request = JSONObject()
                        .put(
                            "msg",
                            JSONObject()
                                .put("cmd", "scan")
                                .put(
                                    "data",
                                    JSONObject().put("account_topic", "reserve")
                                )
                        )
                        .toString()
                        .toByteArray()

                    val packet = DatagramPacket(
                        request,
                        request.size,
                        InetAddress.getByName("255.255.255.255"),
                        4001
                    )

                    socket.send(packet)

                    val buffer = ByteArray(4096)
                    val received = DatagramPacket(buffer, buffer.size)

                    socket.receive(received)

                    responseText = String(
                        received.data,
                        0,
                        received.length
                    )

                    val json = JSONObject(responseText)
                    val data = json.optJSONObject("msg")
                        ?.optJSONObject("data")

                    foundIp = data?.optString("ip")
                        ?.takeIf { it.isNotBlank() }
                        ?: received.address.hostAddress
                }
            } catch (e: Exception) {
                responseText = e.message ?: "Yanıt alınamadı"
            }

            runOnUiThread {
                if (foundIp != null) {
                    deviceIp = foundIp
                    status.text = "● Cihaz bulundu!\nIP: $foundIp"
                } else {
                    status.text =
                        "● Cihaz bulunamadı.\nAyrıntı: $responseText\n\n" +
                        "Cihazın ve tabletin aynı Wi-Fi ağında olduğundan emin ol."
                }
            }
        }
    }

    private fun sendCommand(command: String, data: JSONObject) {
        val ip = deviceIp

        if (ip == null) {
            status.text = "Önce Govee cihazını bulmalısın."
            return
        }

        executor.execute {
            try {
                val message = JSONObject()
                    .put(
                        "msg",
                        JSONObject()
                            .put("cmd", command)
                            .put("data", data)
                    )
                    .toString()
                    .toByteArray()

                DatagramSocket().use { socket ->
                    val packet = DatagramPacket(
                        message,
                        message.size,
                        InetAddress.getByName(ip),
                        4003
                    )
                    socket.send(packet)
                }

                runOnUiThread {
                    status.text = "Komut gönderildi: $command\nIP: $ip"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Komut gönderilemedi: ${e.message}"
                }
            }
        }
    }

    private fun sendColor(color: Int) {
        val data = JSONObject()
            .put(
                "color",
                JSONObject()
                    .put("r", Color.red(color))
                    .put("g", Color.green(color))
                    .put("b", Color.blue(color))
            )
            .put("colorTem", 0)

        sendCommand("colorwc", data)
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
