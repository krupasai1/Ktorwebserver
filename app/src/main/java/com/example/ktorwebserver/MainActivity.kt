package com.example.ktorwebserver
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ktorwebserver.R
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration

class MainActivity: AppCompatActivity() {
    var client: WebSocketServerSession?=null
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val ip= ipaddress(this)
        val iptext= findViewById<TextView>(R.id.ip)
        iptext.text=ip
        lifecycleScope.launch(Dispatchers.IO)
        {
            embeddedServer(Netty,8080)
            {
                install(WebSockets)
                {
                    pingPeriod = java.time.Duration.ofSeconds(15)
                    timeout=java.time.Duration.ofSeconds(15)
                    maxFrameSize=Long.MAX_VALUE
                    masking=false
                }

                routing {
                    webSocket("/ws"){
                        client=this
                        withContext(Dispatchers.Main)
                        {
                            Toast.makeText(this@MainActivity, "connected",Toast.LENGTH_SHORT).show()
                            send(Frame.Text("hello from server"))
                        }
                        for(frame in incoming)
                            when(frame)
                            {
                                is Frame.Binary -> {}
                                is Frame.Text -> {
                                    withContext(Dispatchers.Main)
                                    {
                                        Toast.makeText(this@MainActivity, frame.readText(), Toast.LENGTH_SHORT).show()
                                    }
                                }

                                is Frame.Close -> {withContext(Dispatchers.Main)
                                {
                                    Toast.makeText(this@MainActivity, "closed",Toast.LENGTH_SHORT).show()
                                }}
                                is Frame.Ping -> {}
                                is Frame.Pong -> {}
                            }
                    }
                }
            }.start(wait = false)

        }
        findViewById<Button>(R.id.button).setOnClickListener{
            lifecycleScope.launch(Dispatchers.IO)
            {
                client?.send(Frame.Text("hello"))
            }
        }

    }

    fun ipaddress(context: Context):String?
    {
        try{
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipaddress = wifiInfo.ipAddress
            return String.format("%d.%d.%d.%d", (ipaddress and 0xFF), (ipaddress shr 8 and 0xFF), (ipaddress shr 16 and 0xFF), (ipaddress shr 24 and 0xFF) )


        } catch (e:Exception)
        {
            Toast.makeText(context,"$e", Toast.LENGTH_SHORT).show()
            return null
        }
    }
}


