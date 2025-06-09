package br.unitins.util;

import br.unitins.model.AccessPoint;
import br.unitins.util.windows.WindowsWifiScanner;
import java.util.List;

public class WifiScannerFactory {
    
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS_NAME.contains("windows");
    private static final boolean IS_LINUX = OS_NAME.contains("linux");
    
    public static List<AccessPoint> scanWifiNetworks() {
        if (IS_WINDOWS) {
            return scanWindowsWifi();
        } else if (IS_LINUX) {
            return scanLinuxWifi();
        } else {
            System.err.println("Sistema operacional nao suportado: " + OS_NAME);
            return List.of();
        }
    }
    
    private static List<AccessPoint> scanWindowsWifi() {
        try {
            System.out.println("Usando Windows WLAN API para escaneamento...");
            return WindowsWifiScanner.scanWifiNetworks();
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Erro ao carregar biblioteca Windows WLAN API: " + e.getMessage());
            System.err.println("Verifique se o sistema suporta WLAN API.");
            return List.of();
        } catch (Exception e) {
            System.err.println("Erro no escaneamento Windows: " + e.getMessage());
            return List.of();
        }
    }
    
    private static List<AccessPoint> scanLinuxWifi() {
        try {
            System.out.println("Usando iwlist para escaneamento...");
            return WifiCommandExecutor.scanWifiNetworks();
        } catch (Exception e) {
            System.err.println("Erro no escaneamento Linux: " + e.getMessage());
            return List.of();
        }
    }
    
    public static String getOperatingSystem() {
        return OS_NAME;
    }
    
    public static boolean isWindows() {
        return IS_WINDOWS;
    }
    
    public static boolean isLinux() {
        return IS_LINUX;
    }
} 