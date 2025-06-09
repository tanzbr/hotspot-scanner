@echo off
REM Configurar codificação UTF-8
chcp 65001 >nul 2>&1

REM Configurar variáveis de ambiente para UTF-8 e JNA
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Djna.library.path=C:\Windows\System32
set MAVEN_OPTS=-Dfile.encoding=UTF-8

echo ========================================
echo    HOTSPOT SCANNER - Windows Edition
echo ========================================
echo.

REM Verificar se Java está disponível
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERRO: Java nao encontrado. Instale Java 17+ e adicione ao PATH.
    echo Download: https://adoptium.net/
    pause
    exit /b 1
)

REM Verificar se Maven está disponível
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERRO: Maven nao encontrado. Instale Maven e adicione ao PATH.
    echo Download: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM Verificar se está executando como administrador (recomendado para WLAN API)
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo AVISO: Nao esta executando como administrador.
    echo Para melhor funcionamento da WLAN API, execute como administrador.
    echo.
)

REM Compilar se necessário
if not exist "target\classes\br\unitins\Main.class" (
    echo Compilando projeto...
    mvn clean compile
    if %errorlevel% neq 0 (
        echo ERRO: Falha na compilacao.
        pause
        exit /b 1
    )
)

REM Executar aplicação
echo Executando aplicacao com suporte Windows WLAN API...
echo.
mvn exec:java -Dexec.mainClass="br.unitins.Main"

echo.
echo Aplicacao finalizada.
pause 