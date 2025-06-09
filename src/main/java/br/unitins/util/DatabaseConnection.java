package br.unitins.util;

import br.unitins.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static Connection connection;

    static {
        try {
            Class.forName(DatabaseConfig.DB_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MariaDB JDBC Driver não encontrado", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD
            );
        }
        return connection;
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Criar banco se não existir
            stmt.execute("CREATE DATABASE IF NOT EXISTS hotspot_detector");
            stmt.execute("USE hotspot_detector");
            
            // Dropar tabela se existir para recriar com estrutura correta
            stmt.execute("DROP TABLE IF EXISTS access_points");
            
            // Criar tabela com estrutura correta
            String createTableSQL = """
                CREATE TABLE access_points (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    ssid VARCHAR(255),
                    mac_address VARCHAR(17) NOT NULL,
                    quality_link INT,
                    signal_level INT,
                    channel_number INT,
                    frequency DECIMAL(4,1),
                    last_beacon BIGINT,
                    beacon_interval INT,
                    wifi_security VARCHAR(50),
                    scan_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_scan_time (scan_time),
                    INDEX idx_mac_address (mac_address)
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                """;
            
            stmt.execute(createTableSQL);
            System.out.println("Banco de dados inicializado com sucesso!");
            
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar banco de dados: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Erro ao fechar conexão: " + e.getMessage());
        }
    }
} 