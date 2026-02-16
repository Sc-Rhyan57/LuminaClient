package com.project.lumina.relay.listener

import com.project.lumina.relay.LuminaRelaySession
import com.project.lumina.relay.ConnectionTypeDetector
import com.project.lumina.relay.definition.CameraPresetDefinition
import com.project.lumina.relay.definition.Definitions
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.CameraPresetsPacket
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket
import org.cloudburstmc.protocol.common.NamedDefinition
import org.cloudburstmc.protocol.common.SimpleDefinitionRegistry

@Suppress("MemberVisibilityCanBePrivate")
class GamingPacketHandler(
    val luminaRelaySession: LuminaRelaySession
) : LuminaRelayPacketListener {

    companion object {
        private const val TAG = "GamingPacketHandler"
    }
    
    // Armazena o tipo de conexão detectado
    private var connectionType: ConnectionTypeDetector.ConnectionType? = null

    override fun beforeServerBound(packet: BedrockPacket): Boolean {
        if (packet is StartGamePacket) {
            // NOVO: Detecta tipo de conexão
            connectionType = ConnectionTypeDetector.detectConnectionType(packet)
            
            println("[$TAG] Tipo de conexão detectado: $connectionType")
            println("[$TAG] Server Name: ${packet.serverName}")
            println("[$TAG] Level ID: ${packet.levelId}")
            println("[$TAG] Is Multiplayer: ${packet.isMultiplayerGame}")
            
            // Continua com lógica existente
            Definitions.itemDefinitions = SimpleDefinitionRegistry.builder<ItemDefinition>()
                .addAll(packet.itemDefinitions)
                .build()

            luminaRelaySession.client!!.peer.codecHelper.itemDefinitions = Definitions.itemDefinitions
            luminaRelaySession.server.peer.codecHelper.itemDefinitions = Definitions.itemDefinitions

            if (packet.isBlockNetworkIdsHashed) {
                luminaRelaySession.client!!.peer.codecHelper.blockDefinitions = Definitions.blockDefinitionsHashed
                luminaRelaySession.server.peer.codecHelper.blockDefinitions = Definitions.blockDefinitionsHashed
            } else {
                luminaRelaySession.client!!.peer.codecHelper.blockDefinitions = Definitions.blockDefinitions
                luminaRelaySession.server.peer.codecHelper.blockDefinitions = Definitions.blockDefinitions
            }
            
            // NOVO: Ajusta comportamento baseado no tipo
            when (connectionType) {
                ConnectionTypeDetector.ConnectionType.LOCAL_WORLD -> {
                    handleLocalWorld(packet)
                }
                ConnectionTypeDetector.ConnectionType.DEDICATED_SERVER -> {
                    handleDedicatedServer(packet)
                }
                else -> {
                    // Comportamento padrão (servidor)
                    println("[$TAG] Tipo desconhecido, usando modo servidor")
                }
            }
        }
        
        if (packet is CameraPresetsPacket) {
            val cameraDefinitions =
                SimpleDefinitionRegistry.builder<NamedDefinition>()
                    .addAll(List(packet.presets.size) {
                        CameraPresetDefinition.fromCameraPreset(packet.presets[it], it)
                    })
                    .build()

            luminaRelaySession.client!!.peer.codecHelper.cameraPresetDefinitions = cameraDefinitions
            luminaRelaySession.server.peer.codecHelper.cameraPresetDefinitions = cameraDefinitions
        }
        return false
    }
    
    /**
     * Tratamento específico para mundos locais
     */
    private fun handleLocalWorld(packet: StartGamePacket) {
        println("[$TAG] 🏠 Modo Mundo Local ativado")
        println("[$TAG] Nome do mundo: ${packet.levelName}")
        println("[$TAG] Seed: ${packet.seed}")
        
        // Ajustes específicos para mundos locais:
        // 1. Desabilitar algumas verificações de servidor
        // 2. Permitir comandos locais
        // 3. Ajustar timeouts
        
        // TODO: Implementar ajustes específicos conforme necessário
    }
    
    /**
     * Tratamento específico para servidores dedicados
     */
    private fun handleDedicatedServer(packet: StartGamePacket) {
        println("[$TAG] 🌐 Modo Servidor Dedicado ativado")
        println("[$TAG] Server ID: ${packet.serverId}")
        
        // Mantém comportamento padrão para servidores
    }
    
    /**
     * Retorna o tipo de conexão detectado
     */
    fun getConnectionType(): ConnectionTypeDetector.ConnectionType? {
        return connectionType
    }
}
