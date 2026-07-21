package com.project.lol.proxy

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.security.KeyChain
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Collections
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object LocalProxyManager {
    private const val TAG = "LocalProxy"
    private const val KEYSTORE_FILE = "proxy_ca.p12"
    private const val KEYSTORE_PREFS = "spotilol_secure_prefs"
    private const val KEY_PASSWORD = "keystore_password"
    private const val CA_ALIAS = "spotilol-ca"
    private const val KEYSTORE_TYPE = "PKCS12"

    private var serverSocket: ServerSocket? = null
    @Volatile private var caKeyPair: KeyPair? = null
    @Volatile private var caCert: X509Certificate? = null
    private val threadPool = Executors.newFixedThreadPool(32)

    private val sslContextCache = Collections.synchronizedMap(HashMap<String, SSLContext>())

    // A new CONNECT tunnel to a host we talked to recently can skip the TCP+TLS handshake
    // by using Idle upstream TLS sockets.
    private const val MAX_IDLE_UPSTREAM_PER_HOST = 4
    private val upstreamPool = ConcurrentHashMap<String, ConcurrentLinkedQueue<SSLSocket>>()

    val port: Int get() = serverSocket?.localPort ?: 0
    val isRunning: Boolean get() = serverSocket?.isBound == true && !(serverSocket?.isClosed ?: true)

    private fun getOrCreateKeystorePassword(context: Context): String {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val prefs = EncryptedSharedPreferences.create(
            KEYSTORE_PREFS,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        var password = prefs.getString(KEY_PASSWORD, null)
        if (password == null) {
            val random = SecureRandom()
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            password = Base64.encodeToString(bytes, Base64.NO_WRAP)
            prefs.edit().putString(KEY_PASSWORD, password).apply()
        }
        return password
    }

    fun init(context: Context) {
        val ksFile = File(context.filesDir, KEYSTORE_FILE)
        val password = getOrCreateKeystorePassword(context)

        if (!ksFile.exists()) {
            Log.d(TAG, "Generating new CA certificate")
            generateCA(ksFile, password)
        } else {
            try {
                Log.d(TAG, "Loading existing CA certificate")
                loadCA(ksFile, password)
            } catch (e: Exception) {
                try {
                    Log.d(TAG, "Migrating keystore to new password")
                    val ks = KeyStore.getInstance(KEYSTORE_TYPE)
                    ksFile.inputStream().use { ks.load(it, "".toCharArray()) }
                    val entry = ks.getEntry(CA_ALIAS, KeyStore.PasswordProtection("".toCharArray()))
                        as KeyStore.PrivateKeyEntry
                    caKeyPair = KeyPair(entry.certificate.publicKey, entry.privateKey)
                    caCert = entry.certificate as X509Certificate
                    val newKs = KeyStore.getInstance(KEYSTORE_TYPE)
                    newKs.load(null, null)
                    newKs.setKeyEntry(CA_ALIAS, caKeyPair!!.private, password.toCharArray(), arrayOf(caCert))
                    ksFile.outputStream().use { newKs.store(it, password.toCharArray()) }
                    Log.d(TAG, "Keystore migrated to new password")
                } catch (e2: Exception) {
                    Log.w(TAG, "Failed to load/migrate CA, regenerating", e2)
                    ksFile.delete()
                    generateCA(ksFile, password)
                }
            }
        }
    }

    private fun generateCA(ksFile: File, password: String) {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        caKeyPair = kpg.generateKeyPair()

        val name = X500Name("CN=Spotilol Proxy CA, O=Spotilol")
        val serial = BigInteger.valueOf(System.currentTimeMillis())
        val notBefore = Date()
        val notAfter = Date(notBefore.time + 365L * 24 * 60 * 60 * 1000L * 10)

        val builder = JcaX509v3CertificateBuilder(
            name, serial, notBefore, notAfter, name, caKeyPair!!.public
        )

        builder.addExtension(
            Extension.basicConstraints,
            true,
            BasicConstraints(true)
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .build(caKeyPair!!.private)

        caCert = JcaX509CertificateConverter()
            .getCertificate(builder.build(signer))

        val ks = KeyStore.getInstance(KEYSTORE_TYPE)
        ks.load(null, null)
        ks.setKeyEntry(CA_ALIAS, caKeyPair!!.private, password.toCharArray(), arrayOf(caCert))
        ksFile.outputStream().use { ks.store(it, password.toCharArray()) }

        Log.d(TAG, "CA certificate generated and saved")
    }

    private fun loadCA(ksFile: File, password: String) {
        val ks = KeyStore.getInstance(KEYSTORE_TYPE)
        ksFile.inputStream().use { ks.load(it, password.toCharArray()) }

        val entry = ks.getEntry(CA_ALIAS, KeyStore.PasswordProtection(password.toCharArray()))
            as KeyStore.PrivateKeyEntry
        caKeyPair = KeyPair(entry.certificate.publicKey, entry.privateKey)
        caCert = entry.certificate as X509Certificate

        Log.d(TAG, "CA certificate loaded")
    }

    fun start() {
        if (serverSocket != null) return
        threadPool.execute {
            try {
                serverSocket = ServerSocket(0, 128, java.net.InetAddress.getByName("127.0.0.1"))
                Log.d(TAG, "Proxy started on port ${serverSocket!!.localPort}")

                while (!serverSocket!!.isClosed) {
                    try {
                        val client = serverSocket!!.accept()
                        threadPool.execute { handleConnection(client) }
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start proxy", e)
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
            serverSocket = null
            closeAllPooledUpstreamSockets()
            Log.d(TAG, "Proxy stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping proxy", e)
        }
    }

    private fun poolKey(host: String, port: Int) = "$host:$port"

    /** Hands back an idle previously-verified upstream socket for this host, if one exists. */
    private fun borrowUpstreamSocket(host: String, port: Int): SSLSocket? {
        val queue = upstreamPool[poolKey(host, port)] ?: return null
        while (true) {
            val sock = queue.poll() ?: return null
            if (sock.isClosed || !sock.isConnected || isSocketLikelyDead(sock)) {
                try { sock.close() } catch (_: Exception) {}
                continue
            }
            return sock
        }
    }

    /** Parks a still-usable upstream socket for reuse, or closes it if it can't be reused. */
    private fun releaseUpstreamSocket(host: String, port: Int, sock: SSLSocket, reusable: Boolean) {
        if (!reusable || sock.isClosed || !sock.isConnected) {
            try { sock.close() } catch (_: Exception) {}
            return
        }
        val queue = upstreamPool.computeIfAbsent(poolKey(host, port)) { ConcurrentLinkedQueue() }
        if (queue.size >= MAX_IDLE_UPSTREAM_PER_HOST) {
            try { sock.close() } catch (_: Exception) {}
            return
        }
        queue.offer(sock)
    }

    private fun closeAllPooledUpstreamSockets() {
        upstreamPool.values.forEach { queue ->
            var sock = queue.poll()
            while (sock != null) {
                try { sock.close() } catch (_: Exception) {}
                sock = queue.poll()
            }
        }
        upstreamPool.clear()
    }
	
	    /**
     * Pre-check a pooled socket before writing anything to it.
     * Similar to how Chromium handles its socket pool.
     * If the peer already closed the connection, it returns EOF immediately;
     * If the connection is idle-but-alive, nothing arrives and it times out.
     */
    private fun isSocketLikelyDead(socket: SSLSocket): Boolean {
        val original = try { socket.soTimeout } catch (_: Exception) { return true }
        return try {
            socket.soTimeout = 1
            socket.inputStream.read() == -1  // EOF = peer already closed it
        } catch (_: java.net.SocketTimeoutException) {
            false // nothing waiting - healthy, idle connection, the common case
        } catch (_: Exception) {
            true // any other I/O error on a socket we haven't used yet - don't risk it
        } finally {
            try { socket.soTimeout = original } catch (_: Exception) {}
        }
    }

    /** Opens and fully verifies a brand-new upstream TLS connection. */
    private fun openUpstreamSocket(host: String, port: Int, useHttp2: Boolean): SSLSocket {
        val socket = (SSLSocketFactory.getDefault().createSocket(host, port) as SSLSocket).also {
            if (Build.VERSION.SDK_INT >= 29) {
                val params = it.sslParameters
                params.applicationProtocols = if (useHttp2) arrayOf("h2") else arrayOf("http/1.1")
                it.sslParameters = params
            }
        }

        socket.soTimeout = 30000
        socket.startHandshake()

        val hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
        if (!hostnameVerifier.verify(host, socket.session)) {
            Log.e(TAG, "SECURITY ALERT: Hostname verification failed for $host. Possible network attack.")
            throw SSLPeerUnverifiedException("Cannot verify hostname: $host")
        }

        return socket
    }

    private fun handleConnection(client: Socket) {
        client.soTimeout = 30000
        try {
            val requestLine = readLine(client.inputStream) ?: return

            if (requestLine.startsWith("CONNECT")) {
                val hostPort = requestLine.split(" ")[1]
                val host = hostPort.substringBefore(":")
                val targetPort = hostPort.substringAfter(":").toIntOrNull() ?: 443
                var line: String
                do {
                    line = readLine(client.inputStream) ?: break
                } while (line.isNotEmpty())
                handleConnect(client, host, targetPort)
            } else {
                Log.w(TAG, "Non-CONNECT request, ignoring: $requestLine")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection error", e)
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    private fun handleConnect(client: Socket, host: String, targetPort: Int = 443) {
        Log.d(TAG, "CONNECT $host:$targetPort")

        var clientSSLSocket: SSLSocket? = null
        var upstreamSSLSocket: SSLSocket? = null
        // Optimistic default: only flip to false once we positively know the connection
        // can't be reused
        var reuseUpstream = true
        var fromPool = false

        try {
            client.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
            client.getOutputStream().flush()

            val sslContext = getOrCreateSSLContext(host)

            val clientSocket = sslContext.socketFactory.createSocket(
                client, host, client.port, true
            ) as SSLSocket
            clientSocket.useClientMode = false
            clientSSLSocket = clientSocket

            if (Build.VERSION.SDK_INT >= 29) {
                val params = clientSocket.sslParameters
                params.applicationProtocols = arrayOf("http/1.1")
                clientSocket.sslParameters = params
            }

            clientSocket.startHandshake()

            val negotiatedProtocol = if (Build.VERSION.SDK_INT >= 29) {
                clientSocket.applicationProtocol
            } else null

            // We only ever offer "http/1.1" to the client above, so this is always false currently.
            // Kept so upstream ALPN stays correct if that ever changes.
            val useHttp2 = negotiatedProtocol == "h2"
            Log.d(TAG, "Protocol for $host: ${negotiatedProtocol ?: "none"}")

            val borrowed = borrowUpstreamSocket(host, targetPort)
            var upstream: SSLSocket = if (borrowed != null) {
                fromPool = true
                Log.d(TAG, "Reusing pooled upstream connection for $host")
                borrowed
            } else {
                openUpstreamSocket(host, targetPort, useHttp2)
            }
            upstreamSSLSocket = upstream

            val clientIn = clientSocket.inputStream
            val clientOut = clientSocket.outputStream
            var upstreamIn = upstream.inputStream
            var upstreamOut = upstream.outputStream

            if (useHttp2) {
                reuseUpstream = false
                bidirectionalPipe(clientIn, clientOut, upstreamIn, upstreamOut)
            } else {
                while (true) {
                    val reqHead = readHttpHead(clientIn, null) ?: break
                    val requestMethod = extractMethod(reqHead)

                    modifyRequestHeaders(reqHead)

                    try {
                        writeHead(reqHead, upstreamOut)
                    } catch (e: Exception) {
                        //Retry once on a fresh socket instead of failing the whole tunnel;
                        if (!fromPool) throw e
                        Log.d(TAG, "Pooled upstream for $host was stale, reconnecting")
                        try { upstream.close() } catch (_: Exception) {}
                        upstream = openUpstreamSocket(host, targetPort, useHttp2)
                        upstreamSSLSocket = upstream
                        fromPool = false
                        upstreamIn = upstream.inputStream
                        upstreamOut = upstream.outputStream
                        writeHead(reqHead, upstreamOut)
                    }
                    // outside the retry to reduce chance of truncated body
                    val clientBodyTruncated = pipeBody(clientIn, upstreamOut, reqHead, isResponse = false)
                    if (clientBodyTruncated) {
                        reuseUpstream = false
                        break
                    }

                    val respHead = readHttpHead(upstreamIn, requestMethod)
                    if (respHead == null) {
                        reuseUpstream = false
                        break
                    }

                    val statusCode = extractStatusCode(respHead)
                    writeHead(respHead, clientOut)

                    val upstreamExhausted = pipeBody(upstreamIn, clientOut, respHead, isResponse = true)
                    if (upstreamExhausted) {
                        // Body had no Content-Length/chunked framing, so it was read until
                        // EOF - the upstream socket is now dead regardless of what any
                        // Connection header said. End this tunnel cleanly rather than try
                        // to keep using (or pool) a socket that's already closed.
                        reuseUpstream = false
                        break
                    }

                    if (statusCode == 100 || statusCode == 101) {
                        clientSocket.soTimeout = 0
                        upstream.soTimeout = 0
                        reuseUpstream = false
                        bidirectionalPipe(clientIn, clientOut, upstreamIn, upstreamOut)
                        return
                    }

                    val reqConnection = getHeaderValue(reqHead, "Connection")
                    val respConnection = getHeaderValue(respHead, "Connection")

                    val keepAlive = !reqConnection.equals("close", ignoreCase = true) &&
                        !respConnection.equals("close", ignoreCase = true)

                    reuseUpstream = keepAlive
                    if (!keepAlive) break
                }
            }

        } catch (_: Exception) {
            reuseUpstream = false
        } finally {
            try { clientSSLSocket?.close() } catch (_: Exception) {}
            upstreamSSLSocket?.let { releaseUpstreamSocket(host, targetPort, it, reuseUpstream) }
        }
    }

    private data class HttpHead(
        val statusLine: String,
        val headers: MutableList<Pair<String, String>>,
        val contentLength: Long,
        val isChunked: Boolean,
        val noBody: Boolean
    )

    private fun readHttpHead(input: InputStream, methodHint: String?): HttpHead? {
        val statusLine = readLine(input) ?: return null
        if (statusLine.isEmpty()) return null

        val headers = mutableListOf<Pair<String, String>>()
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            val colon = line.indexOf(':')
            if (colon > 0) {
                headers.add(line.substring(0, colon).trim() to line.substring(colon + 1).trim())
            }
        }

        val isResponse = statusLine.startsWith("HTTP/")
        val noBody: Boolean
        var contentLength = -1L
        var isChunked = false

        if (isResponse) {
            val code = extractStatusCodeFromLine(statusLine)
            noBody = code / 100 == 1 || code == 204 || code == 304 ||
                methodHint.equals("HEAD", ignoreCase = true)
        } else {
            noBody = false
        }
        
        if (!noBody) {
            for ((k, v) in headers) {
                if (k.equals("Content-Length", ignoreCase = true)) {
                    contentLength = v.trim().toLongOrNull() ?: -1L
                }
                if (k.equals("Transfer-Encoding", ignoreCase = true) &&
                    v.equals("chunked", ignoreCase = true)) {
                    isChunked = true
                }
            }
        }

        return HttpHead(statusLine, headers, contentLength, isChunked, noBody)
    }
    private fun writeHead(head: HttpHead, output: OutputStream) {
        val sb = StringBuilder()
        sb.append(head.statusLine).append("\r\n")
        for ((k, v) in head.headers) sb.append(k).append(": ").append(v).append("\r\n")
        sb.append("\r\n")
        output.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
        output.flush()
    }

    /**
     * Returns true if piping this body left the connection unusable (so the caller must
     * not pool it, and must treat this exchange as the last one on this socket).
     */
    private fun pipeBody(
        input: InputStream, output: OutputStream, head: HttpHead, isResponse: Boolean
    ): Boolean {
        return when {
            head.noBody -> false
            head.isChunked -> pipeChunkedBody(input, output)
            head.contentLength > 0 -> { pipeExactBytes(input, output, head.contentLength); false }
            isResponse && head.contentLength < 0 -> {
                // No Content-Length and not chunked on a RESPONSE: per RFC 7230 3.3.3 the
                // body is delimited by the connection closing, not a known length.
                // The old code did nothing here, read to EOF and forward it instead. This exhausts the upstream
                // socket by definition since it can only end via close.
                pipeUntilEof(input, output)
                true
            }
            else -> false
        }
    }

    private fun pipeUntilEof(input: InputStream, output: OutputStream) {
        val buf = ByteArray(8192)
        while (true) {
            val n = input.read(buf)
            if (n == -1) break
            output.write(buf, 0, n)
        }
        output.flush()
    }

    private fun pipeExactBytes(input: InputStream, output: OutputStream, count: Long) {
        val buf = ByteArray(8192)
        var remaining = count
        while (remaining > 0) {
            val n = input.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
            if (n == -1) throw java.io.EOFException("Unexpected end of stream")
            output.write(buf, 0, n)
            remaining -= n
        }
        output.flush()
    }

    private fun pipeChunkedBody(input: InputStream, output: OutputStream): Boolean {
        while (true) {
            val sizeLine = readLine(input) ?: return true
            if (sizeLine.isBlank()) continue
            
            output.write((sizeLine + "\r\n").toByteArray(Charsets.ISO_8859_1))
            val chunkSize = sizeLine.split(";")[0].trim().toLongOrNull(16) ?: return true
            
            if (chunkSize == 0L) {
                // Zero-or-more trailer header lines can follow the final chunk, terminated
                // by a blank line. Consume all of them so
                // nothing is left sitting unread on the socket.
                while (true) {
                    val trailerLine = readLine(input) ?: return true
                    output.write((trailerLine + "\r\n").toByteArray(Charsets.ISO_8859_1))
                    if (trailerLine.isEmpty()) break
                }
                output.flush()
                return false
            }
            
            pipeExactBytes(input, output, chunkSize)
            val crlf = readLine(input) ?: return true
            output.write((crlf + "\r\n").toByteArray(Charsets.ISO_8859_1))
        }
    }

    private fun extractMethod(head: HttpHead): String {
        val parts = head.statusLine.split(" ")
        return if (parts.isNotEmpty() && !parts[0].startsWith("HTTP")) parts[0] else ""
    }

    private fun extractStatusCode(head: HttpHead): Int {
        return extractStatusCodeFromLine(head.statusLine)
    }

    private fun extractStatusCodeFromLine(line: String): Int {
        return try {
            line.split(" ").getOrElse(1) { "" }.toInt()
        } catch (_: Exception) { -1 }
    }

    private fun getHeaderValue(head: HttpHead, name: String): String? {
        for ((k, v) in head.headers) {
            if (k.equals(name, ignoreCase = true)) return v
        }
        return null
    }

    private fun getOrCreateSSLContext(hostname: String): SSLContext {
        sslContextCache[hostname]?.let { return it }

        synchronized(this) {
            sslContextCache[hostname]?.let { return it }

            val (domainCert, domainKeyPair) = generateDomainCert(hostname)

            val ks = KeyStore.getInstance(KEYSTORE_TYPE)
            ks.load(null, null)
            ks.setKeyEntry("leaf", domainKeyPair.private, "spotilol".toCharArray(), arrayOf(domainCert, caCert))

            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(ks, "spotilol".toCharArray())

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(kmf.keyManagers, null, SecureRandom())

            sslContextCache[hostname] = sslContext
            return sslContext
        }
    }

    private fun generateDomainCert(domain: String): Pair<X509Certificate, KeyPair> {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        val domainKeyPair = kpg.generateKeyPair()

        val issuer = X500Name("CN=Spotilol Proxy CA, O=Spotilol")
        val subject = X500Name("CN=$domain")
        val serial = BigInteger.valueOf(System.currentTimeMillis())
        val notBefore = Date()
        val notAfter = Date(notBefore.time + 365L * 24 * 60 * 60 * 1000L)

        val builder = JcaX509v3CertificateBuilder(
            issuer, serial, notBefore, notAfter, subject, domainKeyPair.public
        )

        val san = GeneralNames(GeneralName(GeneralName.dNSName, domain))
        builder.addExtension(Extension.subjectAlternativeName, false, san)

        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .build(caKeyPair!!.private)

        val cert = JcaX509CertificateConverter()
            .getCertificate(builder.build(signer))
        return Pair(cert, domainKeyPair)
    }

    private fun readLine(input: InputStream): String? {
        val baos = ByteArrayOutputStream()
        var prev = 0
        while (true) {
            val b = input.read()
            if (b == -1) return if (baos.size() == 0) null else baos.toString("ISO-8859-1")
            if (b == 10 && prev == 13) {
                val bytes = baos.toByteArray()
                return String(bytes, 0, bytes.size - 1, Charsets.ISO_8859_1)
            }
            baos.write(b)
            prev = b
        }
    }

    private fun modifyRequestHeaders(msg: HttpHead) {
        msg.headers.removeAll { it.first.equals("X-Requested-With", ignoreCase = true) }

        msg.headers.removeAll { it.first.equals("sec-ch-ua", ignoreCase = true) }
        msg.headers.removeAll { it.first.equals("sec-ch-ua-mobile", ignoreCase = true) }
        msg.headers.removeAll { it.first.equals("sec-ch-ua-platform", ignoreCase = true) }

        msg.headers.add("sec-ch-ua" to "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Google Chrome\";v=\"150\"")
        msg.headers.add("sec-ch-ua-mobile" to "?0")
        msg.headers.add("sec-ch-ua-platform" to "\"Windows\"")
    }

    private fun bidirectionalPipe(
        clientIn: InputStream, clientOut: OutputStream,
        upstreamIn: InputStream, upstreamOut: OutputStream
    ) {
        val t1 = Thread {
            try {
                val buf = ByteArray(8192)
                var n: Int
                while (clientIn.read(buf).also { n = it } != -1) {
                    upstreamOut.write(buf, 0, n)
                    upstreamOut.flush()
                }
            } catch (_: Exception) {}
            try { upstreamOut.close() } catch (_: Exception) {}
        }
        val t2 = Thread {
            try {
                val buf = ByteArray(8192)
                var n: Int
                while (upstreamIn.read(buf).also { n = it } != -1) {
                    clientOut.write(buf, 0, n)
                    clientOut.flush()
                }
            } catch (_: Exception) {}
            try { clientOut.close() } catch (_: Exception) {}
        }
        t1.start()
        t2.start()
        t1.join()
        t2.join()
    }

    fun isCAInstalled(): Boolean {
        if (!isRunning) return false

        return try {
            val sock = Socket()
            sock.connect(InetSocketAddress("127.0.0.1", port), 5000)
            sock.soTimeout = 5000

            sock.outputStream.write(
                "CONNECT open.spotify.com:443 HTTP/1.1\r\nHost: open.spotify.com\r\n\r\n".toByteArray()
            )
            sock.outputStream.flush()

            val reader = sock.inputStream.bufferedReader()
            val status = reader.readLine() ?: run {
                sock.close()
                return false
            }

            if (!status.contains("200")) {
                sock.close()
                return false
            }

            var line: String
            do {
                line = reader.readLine() ?: break
            } while (line.isNotEmpty())

            val sslContext = SSLContext.getDefault()
            val ssl = sslContext.socketFactory.createSocket(
                sock, "open.spotify.com", 443, true
            ) as SSLSocket
            ssl.useClientMode = true
            ssl.startHandshake()

            val peerCerts = ssl.session.peerCertificates
            ssl.close()

            Log.d(TAG, "CA is installed (${peerCerts.size} peer certs verified)")
            true
        } catch (e: javax.net.ssl.SSLHandshakeException) {
            Log.d(TAG, "CA not installed: SSL handshake failed")
            false
        } catch (e: Exception) {
            Log.e(TAG, "CA check error", e)
            false
        }
    }

    private fun getPEMContent(): String {
        val base64 = Base64.encodeToString(caCert!!.encoded, Base64.DEFAULT or Base64.NO_WRAP)
        return "-----BEGIN CERTIFICATE-----\n$base64\n-----END CERTIFICATE-----\n"
    }

    fun exportCACert(context: Context): String {
        val pem = getPEMContent()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "Spotilol_CA.pem")
                put(MediaStore.Downloads.MIME_TYPE, "application/x-pem-file")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { os ->
                    os.write(pem.toByteArray())
                }
                val clearPending = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                resolver.update(uri, clearPending, null, null)
                Log.d(TAG, "CA exported to Downloads via MediaStore")
                return "/sdcard/Download/Spotilol_CA.pem"
            }
        } else {
            @Suppress("DEPRECATION")
            val file = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "Spotilol_CA.pem")
            file.writeText(pem)
            Log.d(TAG, "CA exported to ${file.absolutePath}")
            return file.absolutePath
        }

        Log.e(TAG, "Failed to export CA certificate")
        return "export failed"
    }

    fun caCertFileExists(context: Context): Boolean = File(context.filesDir, KEYSTORE_FILE).exists()

    fun installCACert(activity: Activity) {
        try {
            val intent = KeyChain.createInstallIntent()
            intent.putExtra(KeyChain.EXTRA_NAME, "SpotilolMITM")
            intent.putExtra(KeyChain.EXTRA_CERTIFICATE, caCert!!.encoded)
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open KeyChain installer", e)
        }
    }
}
