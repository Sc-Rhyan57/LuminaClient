// Adicione detecção automática ao iniciar o proxy

class LuminaService {
    
    private val lanDiscovery = LANDiscoveryListener()
    private var relay: LuminaRelay? = null
    
    fun startProxy(mode: ProxyMode = ProxyMode.AUTO) {
        when (mode) {
            ProxyMode.AUTO -> startAutoProxy()
            ProxyMode.SERVER -> startServerProxy()
            ProxyMode.LOCAL -> startLocalProxy()
        }
    }
    
    private fun startAutoProxy() {
        // Tenta detectar mundos LAN primeiro
        lanDiscovery.startListening { world ->
            println("Mundo LAN encontrado: ${world.name}")
            // Conecta automaticamente ao primeiro mundo encontrado
            connectToWorld(world.host, world.port)
        }
        
        // Aguarda 3 segundos por mundos LAN
        Thread.sleep(3000)
        
        val worlds = lanDiscovery.getDiscoveredWorlds()
        if (worlds.isNotEmpty()) {
            // Usa mundo LAN
            connectToWorld(worlds.first().host, worlds.first().port)
        } else {
            // Usa configuração padrão de servidor
            startServerProxy()
        }
    }
    
    private fun connectToWorld(host: String, port: Int) {
        relay = LuminaRelay().apply {
            capture(
                remoteAddress = LuminaAddress(host, port)
            ) {
                println("✅ Conectado ao mundo: $host:$port")
            }
        }
    }
    
    private fun startServerProxy() {
        relay = LuminaRelay().apply {
            capture {
                println("✅ Proxy iniciado (modo servidor)")
            }
        }
    }
    
    private fun startLocalProxy() {
        relay = LuminaRelay().apply {
            captureLocalWorld {
                println("✅ Proxy iniciado (modo local)")
            }
        }
    }
    
    enum class ProxyMode {
        AUTO,    // Detecção automática
        SERVER,  // Força modo servidor
        LOCAL    // Força modo local
    }
}
