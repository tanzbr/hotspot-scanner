# Hotspot Scanner

Sistema de monitoramento de redes Wi-Fi em tempo real desenvolvido em Java.

## Funcionalidades

- **Monitoramento em tempo real**: Escaneia redes Wi-Fi a cada 60 segundos
- **Consulta histórica por hora e minuto**: Permite consultar redes por horário específico (HH:mm)
- **Persistência**: Armazena dados no MariaDB com índices otimizados
- **Interface simples**: Menu interativo via console
- **Suporte multiplataforma**: Windows (WLAN API) e Linux (iwlist)

## Informações coletadas

- SSID (Nome da rede)
- Endereço MAC do Access Point
- Qualidade do link (%)
- Nível de sinal (dBm)
- Canal utilizado
- Frequência (GHz)
- Último beacon (ms)
- Intervalo beacon (TUs)
- Versão de segurança Wi-Fi
- Timestamp do escaneamento (truncado para o minuto)

## Pré-requisitos

### Sistema Operacional
- **Linux**: Usa `iwlist` (testado em Ubuntu/Debian)
- **Windows**: Usa Windows WLAN API via JNA (Windows 7+)
- Detecção automática da plataforma

### Software
- Java 17 ou superior
- Maven 3.6+
- MariaDB Server 11.4.5

### Instalação MariaDB

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install mariadb-server mariadb-client

# Iniciar serviço
sudo systemctl start mariadb
sudo systemctl enable mariadb

# Configurar segurança (opcional)
sudo mysql_secure_installation
```

## Configuração

### 1. Configurar MariaDB

```sql
-- Conectar como root
sudo mysql -u root -p

-- Criar banco e usuário
CREATE DATABASE hotspot_scanner;

CREATE USER 'hotspot'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON hotspot_scanner.* TO 'hotspot'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Configurar aplicação

Edite o arquivo `src/main/java/br/unitins/config/DatabaseConfig.java`:

```java
public static final String DB_URL = "jdbc:mariadb://localhost:3306/hotspot_scanner";
public static final String DB_USER = "root"; // ou seu usuário
public static final String DB_PASSWORD = ""; // sua senha
```

## Compilação e Execução

### Linux
```bash
# Compilar
mvn clean compile

# Executar
mvn exec:java -Dexec.mainClass="br.unitins.Main"
```

### Windows
```cmd
# Script otimizado (recomendado)
run-windows.bat

# Ou comando direto
mvn exec:java -Dexec.mainClass="br.unitins.Main"
```

### JAR Executável
```bash
mvn clean package
java -jar target/hotspot-scanner.jar
```

## Uso

### Menu Principal

1. **Monitoramento em tempo real**
   - Inicia escaneamento automático a cada 60 segundos
   - Pressione ENTER para atualizar manualmente
   - Digite 'voltar' para retornar ao menu

2. **Consultar horário específico**
   - Digite uma hora (0-23)
   - Digite os minutos (0-59)
   - Mostra dados do minuto específico do dia atual

3. **Sair**
   - Encerra a aplicação

### Exemplo de Consulta por Horário

```
CONSULTA POR HORARIO ESPECIFICO
============================================================
Digite a hora (0-23): 14
Digite os minutos (0-59): 30

Redes encontradas às 14:30:
```

### Exemplo de Saída (quando redes são encontradas)

```
========================================================================================================================
SSID                 | MAC Address       | Qual | Signal | Ch | Freq   | Beacon   | Interval | Security  
========================================================================================================================
WiFi_Escritorio      | 12:34:56:78:9A:BC |  85% |  -45dBm |  6 |  2.4GHz |   1234ms |  100TUs | WPA2      
NET_CASA_123         | 34:56:78:9A:BC:DE |  72% |  -55dBm | 11 |  2.4GHz |   2345ms |  100TUs | WPA2      
FIBRA_5G             | 56:78:9A:BC:DE:F0 |  65% |  -60dBm | 36 |  5.0GHz |   4567ms |  100TUs | WPA3      
========================================================================================================================
Total: 3 redes encontradas
Ultima atualizacao: 15/12/2024 14:30:00
```

### Exemplo quando nenhuma rede é encontrada

```
Nenhuma rede Wi-Fi encontrada.
Verifique se:
- O comando 'iwlist' esta disponivel no sistema
- Existe uma interface de rede Wi-Fi ativa
- Ha redes Wi-Fi disponiveis na area
```

## Estrutura do Projeto

```
src/main/java/br/unitins/
├── Main.java                    # Classe principal com limpeza de duplicatas
├── model/
│   └── AccessPoint.java         # Modelo de dados com scanTime
├── service/
│   ├── WifiScannerService.java  # Serviço de escaneamento e consultas
│   └── MenuService.java         # Interface com consulta por hora/minuto
├── repository/
│   └── AccessPointRepository.java # Acesso a dados com prevenção de duplicatas
├── util/
│   ├── DatabaseConnection.java   # Conexão com banco e inicialização
│   ├── WifiScannerFactory.java   # Factory para detecção de plataforma
│   ├── WifiCommandExecutor.java  # Execução de comandos Linux
│   └── windows/
│       ├── WindowsWifiScanner.java # Scanner para Windows
│       └── WindowsWlanAPI.java     # Interface JNA para Windows WLAN API
└── config/
    └── DatabaseConfig.java      # Configurações do banco
```

## Banco de Dados

### Tabela: access_points

```sql
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
    INDEX idx_mac_address (mac_address),
    UNIQUE KEY unique_mac_minute (mac_address, scan_time)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Características do Banco

- **Prevenção de duplicatas**: Índice único por MAC address e timestamp
- **Otimização**: Índices em scan_time e mac_address para consultas rápidas
- **UTF-8**: Suporte completo a caracteres especiais em SSIDs
- **Limpeza automática**: Remove duplicatas na inicialização

## Sistema de Prevenção de Duplicatas

### Como Funciona

1. **Truncamento de tempo**: Timestamps são truncados para o minuto (sem segundos)
2. **Verificação antes de salvar**: Sistema verifica se já existe entrada para a rede naquele minuto
3. **Índice único**: Banco de dados impede duplicatas através de constraint
4. **Limpeza automática**: Remove duplicatas existentes na inicialização

### Benefícios

- **Consultas mais rápidas**: Menos dados para processar
- **Economia de espaço**: Banco de dados menor
- **Dados mais limpos**: Uma entrada por rede por minuto
- **Melhor performance**: Índices otimizados

## Troubleshooting

### Erro: "iwlist: command not found"
```bash
# Instalar wireless-tools
sudo apt install wireless-tools
```

### Erro: "No wireless extensions"
```bash
# Verificar interfaces de rede
iwconfig
# Ou usar interface específica
sudo iwlist wlan0 scan
```

### Erro de conexão com MariaDB
- Verificar se o serviço está rodando: `sudo systemctl status mariadb`
- Verificar credenciais em `DatabaseConfig.java`
- Verificar se o usuário tem permissões adequadas

### Suporte Multiplataforma
- **Windows**: Usa Windows WLAN API nativa via JNA
- **Linux**: Usa comando `iwlist` 
- **Detecção automática**: O sistema detecta a plataforma e usa o método apropriado

### Troubleshooting Windows
- Execute como administrador para melhor acesso à WLAN API
- Verifique se o serviço "WLAN AutoConfig" está rodando
- Certifique-se de que há uma interface Wi-Fi ativa

### Comando iwlist não disponível (Linux)
Se o comando `iwlist` não estiver disponível, a aplicação mostrará mensagens de erro apropriadas e não encontrará redes Wi-Fi.

### Problemas de Duplicatas
- O sistema agora previne automaticamente duplicatas
- Na primeira execução, duplicatas existentes são removidas
- Consultas retornam apenas uma entrada por rede por minuto

## Melhorias Implementadas

### v1.1 - Sistema Anti-Duplicatas
- ✅ Prevenção automática de duplicatas por minuto
- ✅ Limpeza de duplicatas existentes na inicialização
- ✅ Consulta por hora e minuto específicos
- ✅ Índice único no banco de dados
- ✅ Otimização de consultas com GROUP BY
- ✅ Truncamento de timestamps para minutos
- ✅ Correção de NullPointerException em scanTime

### v1.0 - Versão Base
- ✅ Monitoramento em tempo real
- ✅ Consulta por hora
- ✅ Suporte Windows e Linux
- ✅ Persistência em MariaDB

## Licença

Este projeto foi desenvolvido para fins educacionais. 
