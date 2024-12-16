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
            val rootNode = rootInActiveWindow
            if (rootNode == null) {
                sendResponse("No active window found.")
                return@Thread
            }

            when (command) {
                "getFullUI" -> {
                    val hierarchy = buildFullHierarchy(rootNode)
                    sendLargeResponse(hierarchy)  // Send FullHierarchy to Python server
                }
                "clickBackButton" -> {
                    clickBackButton(rootNode) // Handle back button click
                }
                else -> {
                    // Default behavior for finding and performing action
                    findAndPerformActionOnMainThread(rootNode, command)
                }
            }
        }.start()
    }

    // Helper to find and perform action on the main thread
    private fun findAndPerformActionOnMainThread(rootNode: AccessibilityNodeInfo, command: String) {
        Handler(Looper.getMainLooper()).post {
            findAndPerformAction(rootNode, command)
        }
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

    private fun buildFullHierarchy(node: AccessibilityNodeInfo?, depth: Int = 0, sb: StringBuilder = StringBuilder()): String {
        if (node == null) return sb.toString()

        val prefix = " ".repeat(depth * 2)
        sb.append(
            "$prefix Node: ${node.className}," +
                    " Text: ${node.text}," +
                    " Clickable: ${node.isClickable}," +
                    " Visible: ${node.isVisibleToUser}," +
                    " Children: ${node.childCount}," +
                    " Scrollable: ${node.isScrollable}\n"
        )

        for (i in 0 until node.childCount) {
            buildFullHierarchy(node.getChild(i), depth + 1, sb)
        }
        return sb.toString()
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

                // send FullHierarchy after Click
                val updatedHierarchy = buildFullHierarchy(rootInActiveWindow)
                sendResponse("Updated UI Hierarchy:\n$updatedHierarchy")
            } else {
                // Traverse up the hierarchy to find a clickable parent
                var parentNode = targetNode.parent
                while (parentNode != null) {
                    if (parentNode.isClickable) {
                        parentNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        sendResponse("Command executed: $command via parent node")

                        // send FullHierarchy after Click
                        val updatedHierarchy = buildFullHierarchy(rootInActiveWindow)
                        sendResponse("Updated UI Hierarchy:\n$updatedHierarchy")
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

    private fun clickBackButton(rootNode: AccessibilityNodeInfo) {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            // check clickable ImageButton
            if (node.className == "android.widget.ImageButton" && node.isClickable) {
                Log.d("AccessibilityService", "Found ImageButton, performing click...")
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                // send FullHierarchy after
                val updatedHierarchy = buildFullHierarchy(rootInActiveWindow)
                sendResponse("Clicked Back Button.\nUpdated UI Hierarchy:\n$updatedHierarchy")
                return
            }

            // add node into queue
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        Log.e("AccessibilityService", "Back Button (ImageButton) not found.")
        sendResponse("Back Button (ImageButton) not found.")
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

    private fun sendLargeResponse(response: String) {
        val chunkSize = 4000  // Maximum logcat size is approximately 4KB.
        var index = 0
        while (index < response.length) {
            val chunk = response.substring(index, minOf(index + chunkSize, response.length))

            // Log each chunk to Logcat
            Log.d("AccessibilityService", "Response Chunk: $chunk")

            // Send each chunk over the socket
            sendResponseToSocket(chunk)
            index += chunkSize
        }
        sendResponseToSocket("[END]")  // Signal the end of the message.
    }

    // Helper function to send data over the socket
    private fun sendResponseToSocket(chunk: String) {
        try {
            socket?.getOutputStream()?.let { outputStream ->
                outputStream.write((chunk + "\n").toByteArray())
                outputStream.flush()
            }
            Log.d("AccessibilityService", "Chunk sent: $chunk")
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error sending chunk: ${e.message}")
        }
    }
}