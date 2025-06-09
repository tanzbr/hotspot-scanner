package br.unitins.util.windows;

import br.unitins.model.AccessPoint;
import br.unitins.util.windows.WindowsWlanAPI.*;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.util.ArrayList;
import java.util.List;

public class WindowsWifiScanner {
    
    public static List<AccessPoint> scanWifiNetworks() {
        List<AccessPoint> accessPoints = new ArrayList<>();
        
        try {
            // Abrir handle para WLAN
            PointerByReference clientHandle = new PointerByReference();
            IntByReference negotiatedVersion = new IntByReference();
            
            int result = WindowsWlanAPI.INSTANCE.WlanOpenHandle(
                WindowsWlanAPI.WLAN_CLIENT_VERSION_2,
                null,
                negotiatedVersion,
                clientHandle
            );
            
            if (result != WindowsWlanAPI.ERROR_SUCCESS) {
                System.err.println("Erro ao abrir handle WLAN: " + result);
                return accessPoints;
            }
            
            try {
                // Enumerar interfaces Wi-Fi
                PointerByReference interfaceListPtr = new PointerByReference();
                result = WindowsWlanAPI.INSTANCE.WlanEnumInterfaces(
                    clientHandle.getValue(),
                    null,
                    interfaceListPtr
                );
                
                if (result != WindowsWlanAPI.ERROR_SUCCESS) {
                    System.err.println("Erro ao enumerar interfaces: " + result);
                    return accessPoints;
                }
                
                try {
                    WLAN_INTERFACE_INFO_LIST interfaceList = new WLAN_INTERFACE_INFO_LIST(interfaceListPtr.getValue());
                    interfaceList.read();
                    
                    // Para cada interface Wi-Fi
                    for (int i = 0; i < interfaceList.dwNumberOfItems; i++) {
                        WLAN_INTERFACE_INFO interfaceInfo = interfaceList.InterfaceInfo[i];
                        
                        // Forçar scan
                        WindowsWlanAPI.INSTANCE.WlanScan(
                            clientHandle.getValue(),
                            interfaceInfo.InterfaceGuid.getPointer(),
                            null, null, null
                        );
                        
                        // Aguardar um pouco para o scan completar
                        Thread.sleep(1000);
                        
                        // Obter lista de redes disponíveis
                        List<AccessPoint> interfaceNetworks = getAvailableNetworks(
                            clientHandle.getValue(),
                            interfaceInfo.InterfaceGuid.getPointer()
                        );
                        
                        accessPoints.addAll(interfaceNetworks);
                    }
                    
                } finally {
                    WindowsWlanAPI.INSTANCE.WlanFreeMemory(interfaceListPtr.getValue());
                }
                
            } finally {
                WindowsWlanAPI.INSTANCE.WlanCloseHandle(clientHandle.getValue(), null);
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao escanear redes Wi-Fi no Windows: " + e.getMessage());
        }
        
        return accessPoints;
    }
    
    private static List<AccessPoint> getAvailableNetworks(Pointer clientHandle, Pointer interfaceGuid) {
        List<AccessPoint> networks = new ArrayList<>();
        
        try {
            PointerByReference networkListPtr = new PointerByReference();
            int result = WindowsWlanAPI.INSTANCE.WlanGetAvailableNetworkList(
                clientHandle,
                interfaceGuid,
                WindowsWlanAPI.WLAN_AVAILABLE_NETWORK_INCLUDE_ALL_ADHOC_PROFILES |
                WindowsWlanAPI.WLAN_AVAILABLE_NETWORK_INCLUDE_ALL_MANUAL_HIDDEN_PROFILES,
                null,
                networkListPtr
            );
            
            if (result != WindowsWlanAPI.ERROR_SUCCESS) {
                System.err.println("Erro ao obter lista de redes: " + result);
                return networks;
            }
            
            try {
                WLAN_AVAILABLE_NETWORK_LIST networkList = new WLAN_AVAILABLE_NETWORK_LIST(networkListPtr.getValue());
                networkList.read();
                
                for (int i = 0; i < networkList.dwNumberOfItems; i++) {
                    WLAN_AVAILABLE_NETWORK network = networkList.Network[i];
                    AccessPoint ap = convertToAccessPoint(network);
                    if (ap != null) {
                        networks.add(ap);
                    }
                }
                
            } finally {
                WindowsWlanAPI.INSTANCE.WlanFreeMemory(networkListPtr.getValue());
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao processar redes disponíveis: " + e.getMessage());
        }
        
        return networks;
    }
    
    private static AccessPoint convertToAccessPoint(WLAN_AVAILABLE_NETWORK network) {
        try {
            AccessPoint ap = new AccessPoint();
            
            // SSID
            String ssid = network.dot11Ssid.getSSID();
            ap.setSsid(ssid.isEmpty() ? null : ssid);
            
            // MAC Address (simulado - WLAN API não fornece diretamente)
            ap.setMacAddress(generateMacAddress(ssid));
            
            // Qualidade do sinal (0-100)
            ap.setQualityLink(network.wlanSignalQuality);
            
            // Nível de sinal em dBm (conversão aproximada)
            int signalLevel = convertQualityToDbm(network.wlanSignalQuality);
            ap.setSignalLevel(signalLevel);
            
            // Canal (estimado baseado no tipo de PHY)
            int channel = estimateChannel(network.dot11PhyTypes[0]);
            ap.setChannel(channel);
            
            // Frequência (baseada no canal estimado)
            double frequency = channel <= 14 ? 2.4 : 5.0;
            ap.setFrequency(frequency);
            
            // Segurança
            String security = getSecurityType(network);
            ap.setWifiSecurity(security);
            
            // Valores padrão para beacon
            ap.setBeaconInterval(100);
            ap.setLastBeacon(System.currentTimeMillis() % 10000);
            
            // Definir scan time para evitar NullPointerException
            ap.setScanTime(java.time.LocalDateTime.now());
            
            return ap;
            
        } catch (Exception e) {
            System.err.println("Erro ao converter rede: " + e.getMessage());
            return null;
        }
    }
    
    private static String generateMacAddress(String ssid) {
        // Gerar MAC baseado no hash do SSID para consistência
        if (ssid == null || ssid.isEmpty()) {
            return "00:00:00:00:00:00";
        }
        
        int hash = ssid.hashCode();
        return String.format("%02X:%02X:%02X:%02X:%02X:%02X",
            (hash >>> 24) & 0xFF,
            (hash >>> 16) & 0xFF,
            (hash >>> 8) & 0xFF,
            hash & 0xFF,
            (hash >>> 12) & 0xFF,
            (hash >>> 4) & 0xFF
        );
    }
    
    private static int convertQualityToDbm(int quality) {
        // Conversão aproximada: 0% = -100dBm, 100% = -30dBm
        return -100 + (quality * 70 / 100);
    }
    
    private static int estimateChannel(int phyType) {
        // Estimativa baseada no tipo de PHY
        // 1 = 802.11a (5GHz), 2 = 802.11b (2.4GHz), 4 = 802.11g (2.4GHz), 8 = 802.11n
        switch (phyType) {
            case 1: return 36; // 5GHz
            case 8: return 6;  // 802.11n (pode ser 2.4 ou 5GHz, assumindo 2.4)
            default: return 6; // 2.4GHz padrão
        }
    }
    
    private static String getSecurityType(WLAN_AVAILABLE_NETWORK network) {
        if (!network.bSecurityEnabled) {
            return "Open";
        }
        
        // Mapear algoritmos de autenticação para tipos de segurança
        switch (network.dot11DefaultAuthAlgorithm) {
            case 1: return "Open";
            case 2: return "WEP";
            case 3: return "WPA";
            case 4: return "WPA";
            case 5: return "WPA2";
            case 6: return "WPA2";
            case 7: return "WPA3";
            case 8: return "WPA3";
            default: return "Unknown";
        }
    }
} 