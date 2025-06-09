#!/bin/bash

echo "=== HOTSPOT SCANNER - SETUP ==="
echo "Configurando ambiente para o sistema de monitoramento Wi-Fi"
echo

# Verificar se está rodando como root para algumas operações
if [[ $EUID -eq 0 ]]; then
   echo "Não execute este script como root. Ele pedirá sudo quando necessário."
   exit 1
fi

# Função para verificar se um comando existe
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Verificar Java
echo "1. Verificando Java..."
if command_exists java; then
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        echo "✓ Java $JAVA_VERSION encontrado"
    else
        echo "✗ Java 17+ necessário. Versão atual: $JAVA_VERSION"
        echo "Instale Java 17+:"
        echo "sudo apt update && sudo apt install openjdk-17-jdk"
        exit 1
    fi
else
    echo "✗ Java não encontrado"
    echo "Instale Java 17+:"
    echo "sudo apt update && sudo apt install openjdk-17-jdk"
    exit 1
fi

# Verificar Maven
echo "2. Verificando Maven..."
if command_exists mvn; then
    echo "✓ Maven encontrado"
else
    echo "✗ Maven não encontrado"
    echo "Instale Maven:"
    echo "sudo apt update && sudo apt install maven"
    exit 1
fi

# Verificar ferramentas de rede
echo "3. Verificando ferramentas de rede..."
if command_exists iwlist; then
    echo "✓ iwlist encontrado"
else
    echo "⚠ iwlist não encontrado. Instalando wireless-tools..."
    sudo apt update
    sudo apt install -y wireless-tools
    if [ $? -eq 0 ]; then
        echo "✓ wireless-tools instalado"
    else
        echo "✗ Erro ao instalar wireless-tools"
        exit 1
    fi
fi

# Verificar MariaDB
echo "4. Verificando MariaDB..."
if command_exists mysql; then
    if systemctl is-active --quiet mariadb; then
        echo "✓ MariaDB está rodando"
    else
        echo "⚠ MariaDB não está rodando. Tentando iniciar..."
        sudo systemctl start mariadb
        if [ $? -eq 0 ]; then
            echo "✓ MariaDB iniciado"
        else
            echo "✗ Erro ao iniciar MariaDB"
        fi
    fi
else
    echo "⚠ MariaDB não encontrado. Instalando..."
    sudo apt update
    sudo apt install -y mariadb-server mariadb-client
    if [ $? -eq 0 ]; then
        echo "✓ MariaDB instalado"
        sudo systemctl start mariadb
        sudo systemctl enable mariadb
        echo "MariaDB configurado para iniciar automaticamente"
    else
        echo "✗ Erro ao instalar MariaDB"
        exit 1
    fi
fi

# Compilar projeto
echo "5. Compilando projeto..."
mvn clean compile
if [ $? -eq 0 ]; then
    echo "✓ Projeto compilado com sucesso"
else
    echo "✗ Erro na compilação"
    exit 1
fi

# Criar script de execução
echo "6. Criando script de execução..."
cat > run.sh << 'EOF'
#!/bin/bash
echo "Iniciando Hotspot Scanner..."
mvn exec:java -Dexec.mainClass="br.unitins.Main"
EOF

chmod +x run.sh
echo "✓ Script de execução criado (run.sh)"

echo
echo "=== SETUP CONCLUÍDO ==="
echo
echo "Para executar a aplicação:"
echo "  ./run.sh"
echo
echo "Ou diretamente:"
echo "  mvn exec:java -Dexec.mainClass=\"br.unitins.Main\""
echo
echo "IMPORTANTE:"
echo "- Configure as credenciais do banco em src/main/java/br/unitins/config/DatabaseConfig.java"
echo "- Execute 'sudo mysql_secure_installation' para configurar segurança do MariaDB (recomendado)"
echo "- O sistema criará automaticamente o banco de dados na primeira execução" 