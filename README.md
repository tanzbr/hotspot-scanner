# Hotspot Scanner

Sistema de monitoramento de redes Wi-Fi em tempo real desenvolvido em Java.

## Funcionalidades

- **Monitoramento em tempo real**: Escaneia redes Wi-Fi a cada 60 segundos
- **Consulta histórica**: Permite consultar redes por horário específico
- **Persistência**: Armazena dados no MariaDB
- **Interface simples**: Menu interativo via console

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

-- Criar usuário (opcional)
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
   - Digite uma hora (0-23) para ver redes daquele horário
   - Mostra dados do dia atual

3. **Sair**
   - Encerra a aplicação

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
Ultima atualizacao: 15/12/2024 14:30:25
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
├── Main.java                    # Classe principal
├── model/
│   └── AccessPoint.java         # Modelo de dados
├── service/
│   ├── WifiScannerService.java  # Serviço de escaneamento
│   └── MenuService.java         # Serviço de interface
├── repository/
│   └── AccessPointRepository.java # Acesso a dados
├── util/
│   ├── DatabaseConnection.java   # Conexão com banco
│   └── WifiCommandExecutor.java  # Execução de comandos
└── config/
    └── DatabaseConfig.java      # Configurações
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
    INDEX idx_mac_address (mac_address)
);
```

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

## Licença

Este projeto foi desenvolvido para fins educacionais. 