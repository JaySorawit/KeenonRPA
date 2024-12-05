package com.example.rpa_accessibilityservice

import android.accessibilityservice.AccessibilityService
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
    private val serverIp = "192.168.1.41" // Replace with Raspberry Pi IP
    private val serverPort = 12345

    override fun onServiceConnected() {
        super.onServiceConnected()
        connectToServer()
    }

    private fun connectToServer() {
        Thread {
            try {
                socket = Socket(serverIp, serverPort)
                writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket?.getOutputStream())), true)
                reader = BufferedReader(InputStreamReader(socket?.getInputStream()))

                Log.d("AccessibilityService", "Connected to server: $serverIp:$serverPort")

                var command: String?
                while (reader?.readLine().also { command = it } != null) {
                    Log.d("AccessibilityService", "Received command: $command")
                    handleCommand(command!!)
                }
            } catch (e: Exception) {
                Log.e("AccessibilityService", "Connection error: ${e.message}")
            }
        }.start()
    }

    private fun closeConnection() {
        try {
            reader?.close()
            writer?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error closing connection: ${e.message}")
        }
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

    override fun onInterrupt() {
        closeConnection()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for this implementation
    }
}