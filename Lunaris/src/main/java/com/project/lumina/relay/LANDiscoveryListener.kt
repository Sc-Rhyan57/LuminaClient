package com.project.lumina.relay

import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import kotlin.concurrent.thread

/**
 * Descobre mundos Bedrock abertos via LAN
 * Baseado em: https://wiki.bedrock.dev/servers/raknet
 * 
 * Minecraft Bedrock usa multicast UDP em 224.0.2.60:4445
 */
class LANDiscoveryListener(
    private val multicastAddress: String = "224.0.2.60",
    private val multicastPort: Int = 4445
) {
    
    data class LANWorld(
        val name: String,
        val host: String,
        val port: Int,
        val motd: String,
        val gamemode: String,
        val playerCount: Int,
        val maxPlayers: Int
    )
    
    private var socket: MulticastSocket? = null
    private var isListening = false
    private var discoveredWorlds = mutableListOf<LANWorld>()
    
    /**
     * Inicia escuta por mundos LAN
     */
    fun startListening(onWorldDiscovered: (LANWorld) -> Unit) {
        if (isListening) return
        
        thread(start = true, name = "LANDiscoveryListener") {
            try {
                socket = MulticastSocket(multicastPort).apply {
                    val group = InetAddress.getByName(multicastAddress)
                    
                    // Junta ao grupo multicast em todas as interfaces
                    NetworkInterface.getNetworkInterfaces().toList().forEach { netIf ->
                        try {
                            joinGroup(group, netIf)
                            println("[LANDiscovery] Escutando em ${netIf.displayName}")
                        } catch (e: Exception) {
                            // Interface pode não suportar multicast
                        }
                    }
                }
                
                isListening = true
                println("[LANDiscovery] Escutando por mundos LAN em $multicastAddress:$multicastPort")
                
                val buffer = ByteArray(1024)
                
                while (isListening) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket?.receive(packet)
                        
                        // Parse o pacote de descoberta
                        val data = String(packet.data, 0, packet.length)
                        parseAndNotify(data, packet.address.hostAddress, onWorldDiscovered)
                        
                    } catch (e: Exception) {
                        if (isListening) {
                            println("[LANDiscovery] Erro ao receber pacote: ${e.message}")
                        }
                    }
                }
                
            } catch (e: Exception) {
                println("[LANDiscovery] Erro ao iniciar: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Para escuta
     */
    fun stopListening() {
        isListening = false
        socket?.close()
        socket = null
        println("[LANDiscovery] Parou de escutar")
    }
    
    /**
     * Parse o formato de descoberta do Bedrock
     * Formato: MCPE ou MCEE;MOTD;Protocol;Version;Players;MaxPlayers;ServerID;MOTD2;Gamemode;GamemodeNum;Port;Port6
     */
    private fun parseAndNotify(data: String, host: String, callback: (LANWorld) -> Unit) {
        try {
            val parts = data.split(";")
            if (parts.size < 12) return
            
            val world = LANWorld(
                name = parts[1],  // MOTD linha 1
                host = host,
                port = parts[10].toIntOrNull() ?: 19132,  // Porta IPv4
                motd = "${parts[1]} - ${parts[7]}",  // MOTD completo
                gamemode = parts[8],  // Modo de jogo
                playerCount = parts[4].toIntOrNull() ?: 0,
                maxPlayers = parts[5].toIntOrNull() ?: 0
            )
            
            // Adiciona apenas se for novo
            if (!discoveredWorlds.any { it.host == world.host && it.port == world.port }) {
                discoveredWorlds.add(world)
                println("[LANDiscovery] 🎮 Mundo descoberto: ${world.name} em ${world.host}:${world.port}")
                callback(world)
            }
            
        } catch (e: Exception) {
            println("[LANDiscovery] Erro ao parsear: ${e.message}")
        }
    }
    
    /**
     * Retorna lista de mundos descobertos
     */
    fun getDiscoveredWorlds(): List<LANWorld> = discoveredWorlds.toList()
}
