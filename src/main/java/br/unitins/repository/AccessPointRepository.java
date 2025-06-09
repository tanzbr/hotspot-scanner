package br.unitins.repository;

import br.unitins.model.AccessPoint;
import br.unitins.util.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AccessPointRepository {
    
    public void saveAccessPoint(AccessPoint ap) throws SQLException {
        // Verificar se scanTime não é null antes de converter
        LocalDateTime scanTime = ap.getScanTime();
        if (scanTime == null) {
            scanTime = LocalDateTime.now();
        }
        
        // Truncar para o minuto (sem segundos) para evitar duplicatas no mesmo minuto
        LocalDateTime truncatedTime = scanTime.withSecond(0).withNano(0);
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Verificar se já existe entrada para esta rede neste minuto
            if (existsInCurrentMinute(conn, ap.getMacAddress(), truncatedTime)) {
                return; // Não salvar se já existe
            }
            
            String sql = """
                INSERT INTO access_points 
                (ssid, mac_address, quality_link, signal_level, channel_number, 
                 frequency, last_beacon, beacon_interval, wifi_security, scan_time) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ap.getSsid());
                stmt.setString(2, ap.getMacAddress());
                stmt.setInt(3, ap.getQualityLink());
                stmt.setInt(4, ap.getSignalLevel());
                stmt.setInt(5, ap.getChannel());
                stmt.setDouble(6, ap.getFrequency());
                stmt.setLong(7, ap.getLastBeacon());
                stmt.setInt(8, ap.getBeaconInterval());
                stmt.setString(9, ap.getWifiSecurity());
                stmt.setTimestamp(10, Timestamp.valueOf(truncatedTime));
                
                stmt.executeUpdate();
            }
        }
    }
    
    public void saveAccessPoints(List<AccessPoint> accessPoints) throws SQLException {
        if (accessPoints.isEmpty()) {
            return;
        }
        
        // Usar INSERT ... ON DUPLICATE KEY UPDATE para evitar duplicatas por minuto
        String sql = """
            INSERT INTO access_points 
            (ssid, mac_address, quality_link, signal_level, channel_number, 
             frequency, last_beacon, beacon_interval, wifi_security, scan_time) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            quality_link = VALUES(quality_link),
            signal_level = VALUES(signal_level),
            channel_number = VALUES(channel_number),
            frequency = VALUES(frequency),
            last_beacon = VALUES(last_beacon),
            beacon_interval = VALUES(beacon_interval),
            wifi_security = VALUES(wifi_security),
            scan_time = VALUES(scan_time)
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false);
            
            for (AccessPoint ap : accessPoints) {
                // Verificar se scanTime não é null antes de converter
                LocalDateTime scanTime = ap.getScanTime();
                if (scanTime == null) {
                    scanTime = LocalDateTime.now();
                }
                
                // Truncar para o minuto (sem segundos) para evitar duplicatas no mesmo minuto
                LocalDateTime truncatedTime = scanTime.withSecond(0).withNano(0);
                
                // Verificar se já existe entrada para esta rede neste minuto
                if (!existsInCurrentMinute(conn, ap.getMacAddress(), truncatedTime)) {
                    stmt.setString(1, ap.getSsid());
                    stmt.setString(2, ap.getMacAddress());
                    stmt.setInt(3, ap.getQualityLink());
                    stmt.setInt(4, ap.getSignalLevel());
                    stmt.setInt(5, ap.getChannel());
                    stmt.setDouble(6, ap.getFrequency());
                    stmt.setLong(7, ap.getLastBeacon());
                    stmt.setInt(8, ap.getBeaconInterval());
                    stmt.setString(9, ap.getWifiSecurity());
                    stmt.setTimestamp(10, Timestamp.valueOf(truncatedTime));
                    
                    stmt.addBatch();
                }
            }
            
            stmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        }
    }
    
    public List<AccessPoint> getLatestAccessPoints() throws SQLException {
        String sql = """
            SELECT ssid, mac_address, quality_link, signal_level, 
                   channel_number, frequency, last_beacon, beacon_interval, 
                   wifi_security, scan_time
            FROM access_points ap1
            WHERE scan_time = (
                SELECT MAX(scan_time) 
                FROM access_points ap2 
                WHERE ap2.mac_address = ap1.mac_address
            )
            GROUP BY mac_address
            ORDER BY quality_link DESC
            """;
        
        return executeQuery(sql);
    }
    
    public List<AccessPoint> getAccessPointsByTime(LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        String sql = """
            SELECT ssid, mac_address, quality_link, signal_level, 
                   channel_number, frequency, last_beacon, beacon_interval, 
                   wifi_security, scan_time
            FROM access_points 
            WHERE scan_time BETWEEN ? AND ?
            ORDER BY scan_time DESC, quality_link DESC
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(startTime));
            stmt.setTimestamp(2, Timestamp.valueOf(endTime));
            
            return executeQuery(stmt);
        }
    }
    
    public List<AccessPoint> getAccessPointsByHour(int hour) throws SQLException {
        LocalDateTime today = LocalDateTime.now().withHour(hour).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = today.plusHours(1);
        
        return getAccessPointsByTime(today, endTime);
    }

    public List<AccessPoint> getAccessPointsByHourAndMinute(int hour, int minute) throws SQLException {
        LocalDateTime startTime = LocalDateTime.now().withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusMinutes(1);
        
        return getAccessPointsByTime(startTime, endTime);
    }
    
    public void cleanOldRecords(int daysToKeep) throws SQLException {
        String sql = "DELETE FROM access_points WHERE scan_time < DATE_SUB(NOW(), INTERVAL ? DAY)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, daysToKeep);
            int deletedRows = stmt.executeUpdate();
            
            if (deletedRows > 0) {
                System.out.println("Removidos " + deletedRows + " registros antigos do banco de dados.");
            }
        }
    }
    
    public void removeDuplicatesByMinute() throws SQLException {
        String sql = """
            DELETE ap1 FROM access_points ap1
            INNER JOIN access_points ap2 
            WHERE ap1.id > ap2.id 
            AND ap1.mac_address = ap2.mac_address 
            AND DATE_FORMAT(ap1.scan_time, '%Y-%m-%d %H:%i') = DATE_FORMAT(ap2.scan_time, '%Y-%m-%d %H:%i')
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int deletedRows = stmt.executeUpdate();
            
            if (deletedRows > 0) {
                System.out.println("Removidas " + deletedRows + " entradas duplicadas do banco de dados.");
            }
        }
    }
    
    private boolean existsInCurrentMinute(Connection conn, String macAddress, LocalDateTime minuteTime) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM access_points 
            WHERE mac_address = ? AND scan_time = ?
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, macAddress);
            stmt.setTimestamp(2, Timestamp.valueOf(minuteTime));
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    
    private List<AccessPoint> executeQuery(String sql) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            return executeQuery(stmt);
        }
    }
    
    private List<AccessPoint> executeQuery(PreparedStatement stmt) throws SQLException {
        List<AccessPoint> accessPoints = new ArrayList<>();
        
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                AccessPoint ap = new AccessPoint();
                ap.setSsid(rs.getString("ssid"));
                ap.setMacAddress(rs.getString("mac_address"));
                ap.setQualityLink(rs.getInt("quality_link"));
                ap.setSignalLevel(rs.getInt("signal_level"));
                ap.setChannel(rs.getInt("channel_number"));
                ap.setFrequency(rs.getDouble("frequency"));
                ap.setLastBeacon(rs.getLong("last_beacon"));
                ap.setBeaconInterval(rs.getInt("beacon_interval"));
                ap.setWifiSecurity(rs.getString("wifi_security"));
                ap.setScanTime(rs.getTimestamp("scan_time").toLocalDateTime());
                
                accessPoints.add(ap);
            }
        }
        
        return accessPoints;
    }
} 