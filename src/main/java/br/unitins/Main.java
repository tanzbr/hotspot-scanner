package br.unitins;

import br.unitins.service.MenuService;
import br.unitins.util.DatabaseConnection;

public class Main {
    public static void main(String[] args) {
        // Configurar codificação UTF-8
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("console.encoding", "UTF-8");
        
        try {
            // Inicializar banco de dados
            System.out.println("Inicializando sistema...");
            DatabaseConnection.initializeDatabase();
            
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