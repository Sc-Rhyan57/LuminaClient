package com.project.lumina.relay

import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket

object ConnectionTypeDetector {
    
    enum class ConnectionType {
        DEDICATED_SERVER,    // Servidor dedicado externo
        LOCAL_WORLD,         // Mundo local/LAN
        UNKNOWN              // Tipo desconhecido (fallback para servidor)
    }
    
    /**
     * Analisa o StartGamePacket para determinar o tipo de conexão
     * 
     * Usa apenas campos que EXISTEM na API:
     * - levelName (String)
     * - levelId (String)
     * - serverId (String) 
     * - seed (long)
     * - isMultiplayerGame (boolean)
     * - worldTemplateId (UUID)
     */
    fun detectConnectionType(packet: StartGamePacket): ConnectionType {
        return try {
            var localWorldScore = 0
            var serverScore = 0
            
            // Análise 1: Level Name
            // Mundos locais geralmente têm nomes genéricos ou vazios
            val levelName = packet.levelName ?: ""
            when {
                levelName.isEmpty() -> localWorldScore += 2
                levelName.length < 5 -> localWorldScore += 1
                levelName.contains("Server", ignoreCase = true) -> serverScore += 2
                levelName.contains("Realm", ignoreCase = true) -> serverScore += 2
                else -> localWorldScore += 1  // Nome customizado sugere mundo local
            }
            
            // Análise 2: Level ID
            // Level ID vazio ou "0" indica mundo local
            val levelId = packet.levelId ?: ""
            when {
                levelId.isEmpty() -> localWorldScore += 3
                levelId == "0" -> localWorldScore += 3
                levelId.length > 10 -> serverScore += 2  // IDs longos = servidor
                else -> localWorldScore += 1
            }
            
            // Análise 3: Server ID
            // Servidores têm IDs únicos complexos
            val serverId = packet.serverId ?: ""
            when {
                serverId.isEmpty() -> localWorldScore += 1
                serverId.length > 20 -> serverScore += 2
                else -> localWorldScore += 1
            }
            
            // Análise 4: Multiplayer Game
            // Mundos locais podem ter isso desabilitado
            if (!packet.isMultiplayerGame) {
                localWorldScore += 3
            } else {
                serverScore += 1
            }
            
            // Análise 5: World Template ID
            // Mundos com templates geralmente são locais
            val templateId = packet.worldTemplateId
            if (templateId != null && templateId.toString() != "00000000-0000-0000-0000-000000000000") {
                localWorldScore += 2
            }
            
            // Análise 6: Seed
            // Seed 0 pode indicar configuração padrão de servidor
            if (packet.seed == 0L) {
                serverScore += 1
            }
            
            println("[ConnectionTypeDetector] Pontuação - Local: $localWorldScore | Servidor: $serverScore")
            
            // Decisão baseada em pontuação
            when {
                localWorldScore > serverScore && localWorldScore >= 5 -> {
                    println("[ConnectionTypeDetector] ✓ Detectado: MUNDO LOCAL")
                    ConnectionType.LOCAL_WORLD
                }
                serverScore > localWorldScore -> {
                    println("[ConnectionTypeDetector] ✓ Detectado: SERVIDOR DEDICADO")
                    ConnectionType.DEDICATED_SERVER
                }
                else -> {
                    println("[ConnectionTypeDetector] ? Detectado: DESCONHECIDO (fallback para servidor)")
                    ConnectionType.UNKNOWN
                }
            }
            
        } catch (e: Exception) {
            println("[ConnectionTypeDetector] ❌ Erro ao detectar tipo: ${e.message}")
            e.printStackTrace()
            ConnectionType.UNKNOWN
        }
    }
    
    /**
     * Verifica se o endereço remoto sugere conexão local
     */
    fun isLocalAddress(host: String): Boolean {
        return when {
            host == "127.0.0.1" -> true
            host == "localhost" -> true
            host == "::1" -> true
            host.startsWith("192.168.") -> true
            host.startsWith("10.") -> true
            host.startsWith("172.") && isPrivateClassB(host) -> true
            host == "0.0.0.0" -> true
            else -> false
        }
    }
    
    private fun isPrivateClassB(host: String): Boolean {
        val parts = host.split(".")
        if (parts.size < 2) return false
        
        return try {
            val secondOctet = parts[1].toInt()
            secondOctet in 16..31
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Imprime informações detalhadas sobre o pacote para debug
     * USA APENAS CAMPOS QUE EXISTEM NA API!
     */
    fun printPacketInfo(packet: StartGamePacket) {
        try {
            println("=== STARTGAMEPACKET INFO ===")
            println("Level Name: ${packet.levelName}")
            println("Level ID: ${packet.levelId}")
            println("Server ID: ${packet.serverId}")
            println("Is Multiplayer: ${packet.isMultiplayerGame}")
            println("Seed: ${packet.seed}")
            println("World Template ID: ${packet.worldTemplateId}")
            println("Game Type: ${packet.levelGameType}")
            println("Difficulty: ${packet.difficulty}")
            println("Default Spawn: ${packet.defaultSpawn}")
            println("Is Editor: ${packet.isEditor}")
            println("Is Server Authoritative: ${packet.serverAuthoritativeMovement}")
            println("============================")
        } catch (e: Exception) {
            println("[ConnectionTypeDetector] Erro ao imprimir info: ${e.message}")
        }
    }
}
