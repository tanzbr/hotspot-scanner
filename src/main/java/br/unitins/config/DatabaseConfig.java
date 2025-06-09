package br.unitins.config;

public class DatabaseConfig {
    public static final String DB_URL = "jdbc:mariadb://localhost:3306/hotspot_detector";
    public static final String DB_USER = "hotspot_user";
    public static final String DB_PASSWORD = "hotspot_pass";
    public static final String DB_DRIVER = "org.mariadb.jdbc.Driver";
    
    // Configurações de pool de conexão
    public static final int MAX_CONNECTIONS = 10;
    public static final int CONNECTION_TIMEOUT = 30000; // 30 segundos
    
    // Configurações da aplicação
    public static final int SCAN_INTERVAL_SECONDS = 60;
    public static final String WIFI_INTERFACE = "wlan0"; // Interface padrão, pode ser alterada
} 