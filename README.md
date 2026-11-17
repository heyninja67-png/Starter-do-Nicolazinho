# Nicolas Keep Alive 🤖

Painel Android para gerenciamento do Robô Nicolas executado no Termux.

## Estado do projeto

Este repositório contém um projeto Android real, em Kotlin/Compose, com:

- Foreground Service e notificação persistente.
- Estrutura modular para Termux, versões, pareamento, monitoramento e notificações.
- Importação/gestão de versões planejada para usar Storage Access Framework.
- Validador de pareamento criptográfico baseado no manifesto da versão.
- Workflow GitHub Actions para APK debug.
- Nenhum segredo ou `.env` do Robô Nicolas incluído.

**Importante:** o app não consegue garantir que o Android mantenha qualquer processo vivo indefinidamente. Fabricantes, economia de bateria, falta de RAM, reinicialização e ausência de internet podem interromper o bot.

## Termux

A integração deve usar somente ações pré-definidas e uma ponte explícita (Termux:Tasker/Run Command quando disponível). O app não deve executar comandos arbitrários extraídos de ZIP.

No dispositivo, confirme que o Termux e a extensão escolhida estão instalados e conceda as permissões solicitadas.

## Segurança de versões

Cada pacote deve conter um manifesto com versão, identificador público de pareamento e SHA-256 do payload. O app calcula a impressão digital e valida o código de pareamento.

Arquivos pessoais do Robô Nicolas (SQLite, `.env`, sessão WhatsApp, credenciais etc.) devem ficar fora do pacote de versão. Antes de ativar uma versão, o gerenciador deve copiar esses dados para um backup seguro e restaurá-los no diretório de dados da versão.

## Compilação no GitHub

1. Suba o projeto para um repositório.
2. Abra **Actions**.
3. Execute **Build Android APK** em **Run workflow**.
4. Abra a execução concluída.
5. Em **Artifacts**, baixe `nicolas-keep-alive-debug-apk`.

Não é necessário secret para APK debug.

## Samsung

Depois de instalar, permita notificações e desative/ajuste a otimização de bateria para o aplicativo e para o Termux. As telas e nomes podem variar conforme a versão do One UI.

## Observação

Este código-base não afirma que o APK foi compilado neste ambiente. A compilação oficial é feita pelo GitHub Actions.
