package com.example.rpa_accessibilityservice

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketException

class MyAccessibilityService : AccessibilityService() {

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        connectToServer() // เริ่มการเชื่อมต่อไปยัง Raspberry Pi
    }

    private fun connectToServer() {
        Thread {
            try {
                // กำหนด IP Address ของ Raspberry Pi
                socket = Socket("192.168.1.41", 12345)
                Log.d("AccessibilityService", "Socket created")

                socket?.getOutputStream()?.let {
                    writer = PrintWriter(it, true)
                    Log.d("AccessibilityService", "Output stream created")
                } ?: Log.e("AccessibilityService", "Failed to get output stream")

                reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
                Log.d("AccessibilityService", "Connected to Raspberry Pi server")

                // อ่านคำสั่งจาก Raspberry Pi
                var command: String?
                while (reader?.readLine().also { command = it } != null) {
                    Log.d("AccessibilityService", "Received command: $command")
                    handleCommand(command!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("AccessibilityService", "Failed to connect to server: ${e.message}")
            } finally {
                closeConnection() // ปิดการเชื่อมต่อเมื่อสิ้นสุดการใช้งาน
            }
        }.start()
    }


    private fun closeConnection() {
        try {
            reader?.close()
            writer?.close()
            socket?.close()
            Log.d("AccessibilityService", "Connection closed")
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error closing connection: ${e.message}")
        }
    }

    override fun onInterrupt() {
        closeConnection() // ปิดการเชื่อมต่อเมื่อ Service ถูกขัดจังหวะ
    }

//    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        event?.source?.let { rootNode ->
//            logNodeInfo(rootNode)
//            findAndClickButton(rootNode)
//        }
//    }

    private fun handleCommand(command: String) {
        Handler(Looper.getMainLooper()).post {
            val rootNode = rootInActiveWindow ?: return@post
            logNodeInfo(rootNode)  // Log UI hierarchy to inspect
            findAndClickButtonByCommand(rootNode, command)
        }
    }


    private fun findAndClickButtonByCommand(rootNode: AccessibilityNodeInfo, command: String) {
        val nodes = rootNode.findAccessibilityNodeInfosByText(command)
        if (nodes.isNotEmpty()) {
            val batteryNode = nodes[0]
            Log.d("AccessibilityService", "Found battery node: ${batteryNode.text}, isClickable: ${batteryNode.isClickable}")

            // หาก Battery Node ไม่สามารถคลิกได้
            if (!batteryNode.isClickable) {
                // ค้นหา Parent Node ที่สูงขึ้น
                var parentNode: AccessibilityNodeInfo? = batteryNode.parent
                while (parentNode != null) {
                    Log.d("AccessibilityService", "Checking parent node: ${parentNode.className}, isClickable: ${parentNode.isClickable}")

                    // ตรวจสอบว่าคลิกได้หรือไม่
                    if (parentNode.isClickable) {
                        // หากพบ Parent Node ที่สามารถคลิกได้ ให้ทำการคลิก
                        parentNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.d("AccessibilityService", "Clicked on parent node: ${parentNode.className}")
                        return
                    }

                    // ขึ้นไปยัง Parent Node ต่อไป
                    parentNode = parentNode.parent
                }
            } else {
                // หาก Battery Node สามารถคลิกได้
                batteryNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d("AccessibilityService", "Clicked on battery node: ${batteryNode.text}")
            }
        } else {
            Log.d("AccessibilityService", "No battery node found")
        }
    }



    private fun logNodeInfo(node: AccessibilityNodeInfo?) {
        // ตรวจสอบว่า Node ไม่เป็น null
        if (node == null) {
            Log.e("AccessibilityService", "Node is null")
            return
        }

        // ทำการ log ข้อมูลของ Node
        Log.d("AccessibilityService", "Node: ${node.className}, Text: ${node.text}, IsClickable: ${node.isClickable}")

        // ตรวจสอบลูกของ Node
        val childCount = node.childCount
        Log.d("AccessibilityService", "Child count: $childCount")

        // วนลูปผ่านลูกของ Node
        for (i in 0 until childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                logNodeInfo(childNode) // เรียกใช้ฟังก์ชันซ้ำเพื่อลงไปลึกใน Hierarchy
            } else {
                Log.e("AccessibilityService", "Child node at index $i is null")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // ไม่ได้ใช้ในโค้ดนี้
    }

}
