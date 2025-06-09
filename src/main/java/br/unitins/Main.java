package br.unitins;

import br.unitins.service.MenuService;
import br.unitins.service.WifiScannerService;
import br.unitins.util.DatabaseConnection;
import br.unitins.util.WifiScannerFactory;

public class Main {
    public static void main(String[] args) {
        // Configurar codificação UTF-8
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("console.encoding", "UTF-8");
        
        try {
            // Mostrar informações da plataforma
            System.out.println("Inicializando sistema...");
            System.out.println("Sistema operacional: " + WifiScannerFactory.getOperatingSystem());
            System.out.println("Suporte Windows: " + WifiScannerFactory.isWindows());
            System.out.println("Suporte Linux: " + WifiScannerFactory.isLinux());
            System.out.println();
            
            // Inicializar banco de dados
            DatabaseConnection.initializeDatabase();
            
            // Limpar duplicatas existentes
            System.out.println("Limpando registros duplicados...");
            WifiScannerService wifiService = new WifiScannerService();
            wifiService.removeDuplicatesByMinute();
            
            // Iniciar aplicação
            MenuService menuService = new MenuService();
            menuService.start();
            
        } catch (Exception e) {
            System.err.println("Erro fatal na aplicação: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Fechar conexão com banco
            DatabaseConnection.closeConnection();
        }
    }
}