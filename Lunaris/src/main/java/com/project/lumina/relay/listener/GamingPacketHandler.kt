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

/**
 * VERSÃO CORRIGIDA - Remove referências a campos inexistentes
 * 
 * ⚠️ CORREÇÕES:
 * - Removido packet.serverName (não existe na API)
 * - Usa apenas packet.levelName, levelId, serverId
 * - Imports corretos
 */
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
            // CORRIGIDO: Detecta tipo de conexão SEM usar serverName
            connectionType = ConnectionTypeDetector.detectConnectionType(packet)
            
            // Debug info usando campos que EXISTEM
            println("[$TAG] ════════════════════════════════════")
            println("[$TAG] Tipo de conexão: $connectionType")
            println("[$TAG] Level Name: ${packet.levelName}")
            println("[$TAG] Level ID: ${packet.levelId}")
            println("[$TAG] Server ID: ${packet.serverId}")
            println("[$TAG] Is Multiplayer: ${packet.isMultiplayerGame}")
            println("[$TAG] Seed: ${packet.seed}")
            println("[$TAG] ════════════════════════════════════")
            
            // Lógica ORIGINAL mantida (não modificar se já funciona!)
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
            
            // NOVO: Ajusta comportamento baseado no tipo (OPCIONAL)
            when (connectionType) {
                ConnectionTypeDetector.ConnectionType.LOCAL_WORLD -> {
                    handleLocalWorld(packet)
                }
                ConnectionTypeDetector.ConnectionType.DEDICATED_SERVER -> {
                    handleDedicatedServer(packet)
                }
                else -> {
                    println("[$TAG] Tipo desconhecido, usando modo padrão")
                }
            }
        }
        
        // Lógica ORIGINAL de CameraPresetsPacket (não modificar!)
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
     * ADICIONE SUA LÓGICA CUSTOMIZADA AQUI
     */
    private fun handleLocalWorld(packet: StartGamePacket) {
        println("[$TAG] 🏠 Modo Mundo Local ativado!")
        println("[$TAG] 📝 Nome do mundo: ${packet.levelName}")
        println("[$TAG] 🌱 Seed: ${packet.seed}")
        
        // TODO: Adicione ajustes específicos para mundos locais aqui
        // Exemplos:
        // - Desabilitar verificações de autenticação de servidor
        // - Permitir comandos locais
        // - Ajustar timeouts
        // - Habilitar features específicas
    }
    
    /**
     * Tratamento específico para servidores dedicados
     * ADICIONE SUA LÓGICA CUSTOMIZADA AQUI
     */
    private fun handleDedicatedServer(packet: StartGamePacket) {
        println("[$TAG] 🌐 Modo Servidor Dedicado ativado!")
        println("[$TAG] 🆔 Server ID: ${packet.serverId}")
        
        // Mantém comportamento padrão para servidores
        // TODO: Adicione lógica customizada se necessário
    }
    
    /**
     * Retorna o tipo de conexão detectado
     */
    fun getConnectionType(): ConnectionTypeDetector.ConnectionType? {
        return connectionType
    }
}
