# Nicolas Keep Alive

Aplicativo Android para controlar o Robô Nicolas que já está instalado no Termux. Este projeto mantém o nome, identidade roxa/dourada, navegação, estrutura de versões e validador de pareamento do aplicativo original.

## Integração real com Termux

O app usa o serviço oficial `com.termux.app.RunCommandService` (ação `com.termux.RUN_COMMAND`) com uma lista fechada de ações: iniciar, parar, reiniciar, status e logs. Ele recebe stdout, stderr e código de saída por PendingIntent e atualiza a interface com o retorno do próprio Termux.

O diretório padrão do bot é `~/Robo-Nicolas`. O app usa os scripts existentes:

- iniciar: `bash ~/Robo-Nicolas/start.sh`
- parar: `bash ~/Robo-Nicolas/stop.sh`

Antes de usar:
1. Abra o Termux e confirme que `~/Robo-Nicolas/start.sh` e `~/Robo-Nicolas/stop.sh` existem.
2. No Termux, edite `~/.termux/termux.properties` e habilite `allow-external-apps=true`; depois execute `termux-reload-settings` ou reinicie o Termux.
3. Nas configurações Android do Nicolas Keep Alive, conceda a permissão adicional **Run commands in Termux environment** (RUN_COMMAND).
4. Mantenha Termux atualizado. O retorno dos comandos via PendingIntent requer Termux 0.109 ou superior.
5. Abra o app e toque em INICIAR.

A API RUN_COMMAND é documentada pelo próprio Termux: https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent

## Sessão WhatsApp existente

O app não apaga, copia, move, renomeia nem reinicializa `auth`, `.env`, banco SQLite ou arquivos pessoais. O comando de iniciar executa o `start.sh` instalado no diretório existente; portanto, o Baileys deve reutilizar a sessão já salva pelo Robô Nicolas.

O app não tenta gerar novo QR ou código de pareamento. Se o WhatsApp tiver invalidado a sessão, a interface informa que será necessária intervenção no Termux; a recuperação deve ser feita pelo fluxo normal do próprio Robô Nicolas.

O indicador **Processo ativo** deriva do PID de inicialização guardado pelo script de controle. O indicador **WhatsApp conectado** somente aparece quando o log da execução atual contém a mensagem de autenticação bem-sucedida usada pelo Robô Nicolas: `Autenticado e conectado ao WhatsApp`. É uma confirmação de log, não uma chamada direta à API do WhatsApp.

## Monitoramento e limites do Android

O serviço de primeiro plano atualiza o status do Termux a cada 60 segundos. A reconexão automática só tenta reiniciar o processo depois que já houve uma conexão WhatsApp confirmada e a resposta posterior informa que o processo parou; há intervalo mínimo entre tentativas. A reconexão da conexão WebSocket/WhatsApp em si permanece sob responsabilidade do Baileys no Robô Nicolas.

O Android e a One UI podem encerrar processos por economia de bateria, memória ou reinicialização do aparelho. Permita notificações e remova restrições de bateria para Nicolas Keep Alive e Termux. Nenhum app Android pode garantir disponibilidade permanente.

## Build

O GitHub Actions executa `gradle check`, `gradle assembleDebug`, publica o APK debug e gera um ZIP do projeto-fonte completo como artefatos separados. Nenhuma sessão, segredo, banco ou arquivo `.env` do bot é empacotado.
