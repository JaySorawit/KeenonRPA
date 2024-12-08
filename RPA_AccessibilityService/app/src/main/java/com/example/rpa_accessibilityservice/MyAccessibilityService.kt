package com.example.rpa_accessibilityservice

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.*
import java.net.Socket

class MyAccessibilityService : AccessibilityService() {

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var isRunning = false

    private val disconnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.rpa_accessibilityservice.DISCONNECT") {
                closeConnection()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true

        // Retrieve IP and Port from SharedPreferences
        val sharedPreferences = getSharedPreferences("RPA_PREFS", Context.MODE_PRIVATE)
        val serverIp = sharedPreferences.getString("IP", null)
        val serverPort = sharedPreferences.getInt("PORT", -1)

        if (serverIp != null && serverPort != -1) {
            connectToServer(serverIp, serverPort)
        } else {
            Log.e("AccessibilityService", "IP or Port not found in SharedPreferences")
        }
    }

    private fun connectToServer(ip: String, port: Int) {
        Thread {
            try {
                socket = Socket(ip, port)
                writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket?.getOutputStream())), true)
                reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
                Log.d("AccessibilityService", "Connected to server: $ip:$port")

                // Read and handle commands
                while (isRunning) {
                    val command = reader?.readLine()
                    if (command != null) {
                        Log.d("AccessibilityService", "Received command: $command")
                        handleCommand(command)
                    } else {
                        Log.d("AccessibilityService", "No command received. Closing connection.")
                        break
                    }
                }
            } catch (e: IOException) {
                Log.e("AccessibilityService", "Connection error: ${e.message}")
            } finally {
                closeConnection() // Ensure the socket is closed on error or disconnect
            }
        }.start()
    }

    private fun closeConnection() {
        try {
            isRunning = false
            reader?.close()
            writer?.close()
            socket?.close()
            socket = null
            Log.d("AccessibilityService", "Disconnected from server")
        } catch (e: IOException) {
            Log.e("AccessibilityService", "Error closing connection: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        closeConnection()
        Log.d("AccessibilityService", "Service destroyed")
    }

    override fun onInterrupt() {
        closeConnection()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for this implementation
    }

    private fun handleCommand(command: String) {
        Thread {
            val rootNode = rootInActiveWindow ?: return@Thread
            logFullHierarchy(rootNode) // Log UI hierarchy for debugging
            Handler(Looper.getMainLooper()).post {
                findAndPerformAction(rootNode, command)
            }
        }.start()
    }

    private fun logFullHierarchy(node: AccessibilityNodeInfo?, depth: Int = 0) {
        if (node == null) {
            Log.e("UIHierarchy", "Node is null at depth $depth")
            return
        }

        val prefix = " ".repeat(depth * 2)
        Log.d(
            "UIHierarchy",
            "$prefix Node: ${node.className}," +
                    "Text: ${node.text}," +
                    "Clickable: ${node.isClickable}," +
                    "Visible: ${node.isVisibleToUser}," +
                    "Children: ${node.childCount}," +
                    "Scrollable: ${node.isScrollable}"
        )

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child == null) {
                Log.e("UIHierarchy", "$prefix Child at index $i is null.")
                continue
            }
            logFullHierarchy(child, depth + 1)
        }
    }

    private fun findNodeByPartialText(rootNode: AccessibilityNodeInfo, keyword: String): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            // Match text partially
            if (node.text?.contains(keyword, true) == true || node.contentDescription?.contains(keyword, true) == true) {
                return node
            }

            // Add children to the queue
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findAndPerformAction(rootNode: AccessibilityNodeInfo, command: String) {
        val targetNode = findNodeByPartialText(rootNode, command)

        if (targetNode != null) {
            // Log the node regardless of visibility
            logNode(targetNode)

            // Perform action if node is clickable
            if (targetNode.isClickable) {
                targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                sendResponse("Command executed: $command")
            } else {
                // Traverse up the hierarchy to find a clickable parent
                var parentNode = targetNode.parent
                while (parentNode != null) {
                    if (parentNode.isClickable) {
                        parentNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        sendResponse("Command executed: $command via parent node")
                        return
                    }
                    parentNode = parentNode.parent
                }
                Log.e("AccessibilityService", "No clickable parent found for command: $command")
            }
        } else {
            Log.e("AccessibilityService", "Command not found in UI: $command")
            sendResponse("Command not found in UI: $command")
        }
    }

    // Log the node regardless of visibility
    private fun logNode(node: AccessibilityNodeInfo) {
        Log.d("AccessibilityService",
            "Node Text: ${node.text} | " +
                    "Class: ${node.className} | " +
                    "Clickable: ${node.isClickable} | " +
                    "Visible: ${node.isVisibleToUser} |" +
                    "Scrollable: ${node.isScrollable}")
    }

    // If want to click only UI visible to user
    private fun findAndPerformActionV2(rootNode: AccessibilityNodeInfo, command: String) {
        val nodes = rootNode.findAccessibilityNodeInfosByText(command)
        if (nodes.isNotEmpty()) {
            val targetNode = nodes[0]

            if (targetNode.isVisibleToUser && targetNode.isClickable) {
                targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                sendResponse("Command executed: $command")
            } else {
                // Traverse up the hierarchy to find a clickable parent
                var parentNode = targetNode.parent
                while (parentNode != null) {
                    if (parentNode.isVisibleToUser && parentNode.isClickable) {
                        parentNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        sendResponse("Command executed: $command via parent node")
                        return
                    }
                    parentNode = parentNode.parent
                }
                Log.e("AccessibilityService", "No clickable parent found for command: $command")
            }
        } else {
            Log.e("AccessibilityService", "Command not found in UI: $command")
        }
    }

    private fun sendResponse(response: String) {
        Thread {
            try {
                writer?.println(response)
                writer?.flush()
                Log.d("AccessibilityService", "Response sent: $response")
            } catch (e: Exception) {
                Log.e("AccessibilityService", "Error sending response: ${e.message}")
            }
        }.start()
    }
}