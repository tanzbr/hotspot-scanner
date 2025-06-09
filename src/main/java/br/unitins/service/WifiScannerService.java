package br.unitins.service;

import br.unitins.model.AccessPoint;
import br.unitins.repository.AccessPointRepository;
import br.unitins.util.WifiScannerFactory;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WifiScannerService {
    private final AccessPointRepository repository;
    private final ScheduledExecutorService scheduler;
    private boolean isScanning = false;

    public WifiScannerService() {
        this.repository = new AccessPointRepository();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public List<AccessPoint> scanAndSaveAccessPoints() {
        try {
            System.out.println("Escaneando redes Wi-Fi...");
            List<AccessPoint> accessPoints = WifiScannerFactory.scanWifiNetworks();
            
            if (!accessPoints.isEmpty()) {
                repository.saveAccessPoints(accessPoints);
                System.out.println("Encontradas " + accessPoints.size() + " redes Wi-Fi");
            } else {
                System.out.println("Nenhuma rede Wi-Fi encontrada no escaneamento");
            }
            
            return accessPoints;
            
        } catch (SQLException e) {
            System.err.println("Erro ao salvar dados no banco: " + e.getMessage());
            // Retorna lista vazia se houver erro no banco
            return new ArrayList<>();
        }
    }

    public List<AccessPoint> getLatestAccessPoints() {
        try {
            return repository.getLatestAccessPoints();
        } catch (SQLException e) {
            System.err.println("Erro ao buscar dados do banco: " + e.getMessage());
            return List.of();
        }
    }

    public List<AccessPoint> getAccessPointsByHour(int hour) {
        try {
            if (hour < 0 || hour > 23) {
                throw new IllegalArgumentException("Hora deve estar entre 0 e 23");
            }
            return repository.getAccessPointsByHour(hour);
        } catch (SQLException e) {
            System.err.println("Erro ao buscar dados por horario: " + e.getMessage());
            return List.of();
        }
    }

    public List<AccessPoint> getAccessPointsByHourAndMinute(int hour, int minute) {
        try {
            if (hour < 0 || hour > 23) {
                throw new IllegalArgumentException("Hora deve estar entre 0 e 23");
            }
            if (minute < 0 || minute > 59) {
                throw new IllegalArgumentException("Minutos devem estar entre 0 e 59");
            }
            return repository.getAccessPointsByHourAndMinute(hour, minute);
        } catch (SQLException e) {
            System.err.println("Erro ao buscar dados por horario e minuto: " + e.getMessage());
            return List.of();
        }
    }

    public List<AccessPoint> getAccessPointsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            return repository.getAccessPointsByTime(startTime, endTime);
        } catch (SQLException e) {
            System.err.println("Erro ao buscar dados por periodo: " + e.getMessage());
            return List.of();
        }
    }

    public void startRealTimeMonitoring() {
        if (isScanning) {
            System.out.println("Monitoramento ja esta ativo!");
            return;
        }

        isScanning = true;
        System.out.println("Iniciando monitoramento em tempo real...");
        
        // Primeira execução imediata
        scanAndSaveAccessPoints();
        
        // Agendar execuções periódicas a cada 60 segundos
        scheduler.scheduleAtFixedRate(() -> {
            if (isScanning) {
                scanAndSaveAccessPoints();
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    public void stopRealTimeMonitoring() {
        isScanning = false;
        System.out.println("Monitoramento em tempo real parado.");
    }

    public boolean isMonitoring() {
        return isScanning;
    }

    public void cleanOldRecords() {
        try {
            repository.cleanOldRecords(1); // Manter apenas dados do dia atual
        } catch (SQLException e) {
            System.err.println("Erro ao limpar registros antigos: " + e.getMessage());
        }
    }
    
    public void removeDuplicatesByMinute() {
        try {
            repository.removeDuplicatesByMinute();
        } catch (SQLException e) {
            System.err.println("Erro ao remover duplicatas: " + e.getMessage());
        }
    }

    public void shutdown() {
        stopRealTimeMonitoring();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 