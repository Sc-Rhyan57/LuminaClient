// Adicione este método à classe LuminaRelay

/**
 * Modo especial para mundos locais
 * Conecta ao localhost:19132 por padrão
 */
fun captureLocalWorld(
    localPort: Int = 19132,
    onSessionCreated: LuminaRelaySession.() -> Unit
): LuminaRelay {
    return capture(
        remoteAddress = LuminaAddress("127.0.0.1", localPort),
        onSessionCreated = onSessionCreated
    )
}

/**
 * Detecta automaticamente se deve usar modo local ou remoto
 */
fun captureAuto(
    remoteAddress: LuminaAddress? = null,
    onSessionCreated: LuminaRelaySession.() -> Unit
): LuminaRelay {
    val targetAddress = remoteAddress ?: run {
        // Tenta detectar automaticamente
        val nativeRemote = LuminaAddress(getNativeRemoteIp(), getNativeRemotePort())
        
        if (ConnectionTypeDetector.isLocalAddress(nativeRemote.host)) {
            println("[LuminaRelay] Endereço local detectado: ${nativeRemote.host}")
            nativeRemote
        } else {
            println("[LuminaRelay] Endereço remoto detectado: ${nativeRemote.host}")
            nativeRemote
        }
    }
    
    return capture(targetAddress, onSessionCreated)
}
