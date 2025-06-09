package br.unitins.util;

import br.unitins.model.AccessPoint;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WifiCommandExecutor {
    
    public static List<AccessPoint> scanWifiNetworks() {
        List<AccessPoint> accessPoints = new ArrayList<>();
        
        try {
            // Usar iwlist para escanear redes Wi-Fi
            Process process = Runtime.getRuntime().exec("iwlist scan");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line;
            AccessPoint currentAP = null;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Nova célula (novo AP)
                if (line.contains("Cell") && line.contains("Address:")) {
                    if (currentAP != null) {
                        accessPoints.add(currentAP);
                    }
                    currentAP = new AccessPoint();
                    String macAddress = extractMacAddress(line);
                    currentAP.setMacAddress(macAddress);
                }
                
                if (currentAP != null) {
                    // SSID
                    if (line.contains("ESSID:")) {
                        String ssid = extractSSID(line);
                        currentAP.setSsid(ssid);
                    }
                    
                    // Qualidade e Sinal
                    if (line.contains("Quality=") && line.contains("Signal level=")) {
                        int quality = extractQuality(line);
                        int signalLevel = extractSignalLevel(line);
                        currentAP.setQualityLink(quality);
                        currentAP.setSignalLevel(signalLevel);
                    }
                    
                    // Frequência e Canal
                    if (line.contains("Frequency:")) {
                        double frequency = extractFrequency(line);
                        int channel = extractChannel(line);
                        currentAP.setFrequency(frequency);
                        currentAP.setChannel(channel);
                    }
                    
                    // Segurança
                    if (line.contains("Encryption key:")) {
                        String security = line.contains("on") ? "WEP" : "Open";
                        currentAP.setWifiSecurity(security);
                    }
                    
                    if (line.contains("IE: IEEE 802.11i/WPA2")) {
                        currentAP.setWifiSecurity("WPA2");
                    }
                    
                    if (line.contains("IE: WPA")) {
                        currentAP.setWifiSecurity("WPA");
                    }
                    
                    // Beacon interval - tentar extrair ou usar valor padrão
                    if (line.contains("Beacon Interval:")) {
                        int beaconInterval = extractBeaconInterval(line);
                        currentAP.setBeaconInterval(beaconInterval);
                    }
                    
                    // Last seen (simulado baseado no tempo atual)
                    if (line.contains("Last beacon:") || line.contains("Extra:")) {
                        currentAP.setLastBeacon(System.currentTimeMillis() % 10000); // Simulado
                    }
                    
                    // Definir valores padrão se não foram definidos
                    if (currentAP.getBeaconInterval() == 0) {
                        currentAP.setBeaconInterval(100); // Valor padrão típico
                    }
                    if (currentAP.getLastBeacon() == 0) {
                        currentAP.setLastBeacon(System.currentTimeMillis() % 10000);
                    }
                }
            }
            
            // Adicionar o último AP
            if (currentAP != null) {
                accessPoints.add(currentAP);
            }
            
            process.waitFor();
            reader.close();
            
        } catch (IOException | InterruptedException e) {
            System.err.println("Erro ao executar comando iwlist: " + e.getMessage());
            System.err.println("Comando iwlist nao disponivel. Nenhuma rede Wi-Fi encontrada.");
            return new ArrayList<>();
        }
        
        return accessPoints;
    }
    
    private static String extractMacAddress(String line) {
        Pattern pattern = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group() : "00:00:00:00:00:00";
    }
    
    private static String extractSSID(String line) {
        Pattern pattern = Pattern.compile("ESSID:\"(.*)\"");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private static int extractQuality(String line) {
        Pattern pattern = Pattern.compile("Quality=(\\d+)/(\\d+)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            int current = Integer.parseInt(matcher.group(1));
            int max = Integer.parseInt(matcher.group(2));
            return (current * 100) / max;
        }
        return 0;
    }
    
    private static int extractSignalLevel(String line) {
        Pattern pattern = Pattern.compile("Signal level=(-?\\d+)");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -100;
    }
    
    private static double extractFrequency(String line) {
        Pattern pattern = Pattern.compile("Frequency:(\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : 2.4;
    }
    
    private static int extractChannel(String line) {
        Pattern pattern = Pattern.compile("Channel (\\d+)");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
    }
    
    private static int extractBeaconInterval(String line) {
        Pattern pattern = Pattern.compile("Beacon Interval:(\\d+)");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 100;
    }
    

} 