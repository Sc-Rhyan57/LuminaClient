package com.project.lumina.relay

import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket

/**
 * Detecta se a conexão é para um servidor dedicado ou mundo local
 */
object ConnectionTypeDetector {
    
    enum class ConnectionType {
        DEDICATED_SERVER,    // Servidor dedicado externo
        LOCAL_WORLD,         // Mundo local/LAN
        UNKNOWN              // Tipo desconhecido (fallback para servidor)
    }
    
    /**
     * Analisa o StartGamePacket para determinar o tipo de conexão
     * 
     * Baseado em:
     * - https://wiki.bedrock.dev/servers/raknet
     * - https://github.com/brokiem/BedrockReplay
     * - Testes empíricos com mundos locais vs servidores
     */
    fun detectConnectionType(packet: StartGamePacket): ConnectionType {
        return try {
            // Mundos locais geralmente têm características específicas:
            
            // 1. Server Name começa com "Minecraft World" ou similar
            val serverName = packet.serverName ?: ""
            if (serverName.contains("Minecraft World", ignoreCase = true) || 
                serverName.contains("Local Game", ignoreCase = true)) {
                return ConnectionType.LOCAL_WORLD
            }
            
            // 2. Level ID geralmente é vazio ou padrão em mundos locais
            val levelId = packet.levelId ?: ""
            if (levelId.isEmpty() || levelId == "0") {
                return ConnectionType.LOCAL_WORLD
            }
            
            // 3. Server Engine Version pode indicar mundo local
            val serverEngine = packet.serverEngine ?: ""
            if (serverEngine.isEmpty()) {
                return ConnectionType.LOCAL_WORLD
            }
            
            // 4. Verifica se multiplayer está habilitado
            // Mundos locais sem LAN aberto teriam isso desabilitado
            if (!packet.isMultiplayerGame) {
                return ConnectionType.LOCAL_WORLD
            }
            
            // 5. Porta do servidor - mundos locais usam portas aleatórias ou 19132
            // mas em contexto diferente de servidores dedicados
            
            // Se nenhuma característica de mundo local for encontrada,
            // assume que é servidor dedicado
            ConnectionType.DEDICATED_SERVER
            
        } catch (e: Exception) {
            println("[ConnectionTypeDetector] Erro ao detectar tipo: ${e.message}")
            // Em caso de erro, assume servidor dedicado por segurança
            ConnectionType.UNKNOWN
        }
    }
    
    /**
     * Verifica se o endereço remoto sugere conexão local
     */
    fun isLocalAddress(host: String): Boolean {
        return host == "127.0.0.1" || 
               host == "localhost" ||
               host.startsWith("192.168.") ||
               host.startsWith("10.") ||
               host.startsWith("172.")
    }
}
