package br.unitins.service;

import br.unitins.model.AccessPoint;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class MenuService {
    private final WifiScannerService wifiService;
    private final Scanner scanner;
    private boolean running = true;

    public MenuService() {
        this.wifiService = new WifiScannerService();
        this.scanner = new Scanner(System.in);
        
        // Configurar codificação UTF-8 para o console
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.err.println("Aviso: Não foi possível configurar UTF-8 para o console");
        }
    }

    public void start() {
        System.out.println("=== HOTSPOT SCANNER ===");
        System.out.println("Sistema de Monitoramento de Redes Wi-Fi");
        System.out.println();

        while (running) {
            showMainMenu();
            int option = getMenuOption();
            processMenuOption(option);
        }

        cleanup();
    }

    private void showMainMenu() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("MENU PRINCIPAL");
        System.out.println("=".repeat(60));
        System.out.println("1 - Monitoramento em tempo real");
        System.out.println("2 - Consultar horario especifico");
        System.out.println("3 - Sair");
        System.out.println("=".repeat(60));
        System.out.print("Escolha uma opcao: ");
    }

    private int getMenuOption() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void processMenuOption(int option) {
        switch (option) {
            case 1 -> handleRealTimeMonitoring();
            case 2 -> handleHistoricalQuery();
            case 3 -> {
                System.out.println("Encerrando aplicacao...");
                running = false;
            }
            default -> System.out.println("Opcao invalida! Tente novamente.");
        }
    }

    private void handleRealTimeMonitoring() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("MONITORAMENTO EM TEMPO REAL");
        System.out.println("=".repeat(60));
        System.out.println("Pressione ENTER para atualizar manualmente ou digite 'voltar' para retornar");
        System.out.println();

        wifiService.startRealTimeMonitoring();

        while (true) {
            displayAccessPoints(wifiService.getLatestAccessPoints());
            
            System.out.println("\nPróxima atualização automática em 60 segundos...");
            System.out.print("Pressione ENTER para atualizar agora ou digite 'voltar': ");
            
            String input = scanner.nextLine().trim().toLowerCase();
            
            if ("voltar".equals(input)) {
                wifiService.stopRealTimeMonitoring();
                break;
            }
            
            if (input.isEmpty()) {
                // Força uma nova varredura
                wifiService.scanAndSaveAccessPoints();
            }
        }
    }

    private void handleHistoricalQuery() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CONSULTA POR HORARIO ESPECIFICO");
        System.out.println("=".repeat(60));
        
        try {
            System.out.print("Digite a hora (0-23): ");
            int hour = Integer.parseInt(scanner.nextLine().trim());
            
            if (hour < 0 || hour > 23) {
                System.out.println("Hora invalida! Deve estar entre 0 e 23.");
                return;
            }
            
            System.out.print("Digite os minutos (0-59): ");
            int minute = Integer.parseInt(scanner.nextLine().trim());
            
            if (minute < 0 || minute > 59) {
                System.out.println("Minutos invalidos! Devem estar entre 0 e 59.");
                return;
            }
            
            List<AccessPoint> accessPoints = wifiService.getAccessPointsByHourAndMinute(hour, minute);
            
            if (accessPoints.isEmpty()) {
                System.out.println("Nenhum dado encontrado para " + 
                    String.format("%02d:%02d", hour, minute) + " de hoje.");
            } else {
                System.out.println("\nRedes encontradas às " + 
                    String.format("%02d:%02d", hour, minute) + ":");
                displayAccessPoints(accessPoints);
            }
            
            System.out.println("\nPressione ENTER para continuar...");
            scanner.nextLine();
            
        } catch (NumberFormatException e) {
            System.out.println("Valor invalido! Digite apenas numeros.");
        }
    }

    private void displayAccessPoints(List<AccessPoint> accessPoints) {
        if (accessPoints.isEmpty()) {
            System.out.println("\nNenhuma rede Wi-Fi encontrada.");
            System.out.println("Verifique se:");
            System.out.println("- O comando 'iwlist' esta disponivel no sistema");
            System.out.println("- Existe uma interface de rede Wi-Fi ativa");
            System.out.println("- Ha redes Wi-Fi disponiveis na area");
            return;
        }

        System.out.println("\n" + "=".repeat(120));
        System.out.printf("%-20s | %-17s | %-4s | %-6s | %-2s | %-6s | %-8s | %-6s | %-10s%n",
                "SSID", "MAC Address", "Qual", "Signal", "Ch", "Freq", "Beacon", "Interval", "Security");
        System.out.println("=".repeat(120));

        for (AccessPoint ap : accessPoints) {
            System.out.println(ap.toString());
        }
        
        System.out.println("=".repeat(120));
        System.out.println("Total: " + accessPoints.size() + " redes encontradas");
        
        if (!accessPoints.isEmpty()) {
            LocalDateTime scanTime = accessPoints.get(0).getScanTime();
            System.out.println("Ultima atualizacao: " + 
                scanTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        }
    }

    private void cleanup() {
        wifiService.shutdown();
        scanner.close();
        System.out.println("Aplicacao encerrada com sucesso!");
    }
} 