# Correções do Nicolas Keep Alive

## Integração real
- Usa os scripts originais do ZIP do Robô Nicolas: `start-bg.sh` e `stop.sh`.
- O comando STATUS consulta `data/bot.pid`, valida o diretório e o comando do PID, testa o gateway com `python3 -m nicolas health`, verifica `auth/creds.json` e analisa `logs/supervisor.log`.
- O comando LOGS mostra `logs/supervisor.log` e `logs/bot.log`.
- Resultado do Termux recebe stdout, stderr, exitCode, err e errmsg.
- A interface inclui o procedimento para ativar `allow-external-apps=true` e para conceder RUN_COMMAND.
- Serviço em primeiro plano consulta o estado a cada 60 segundos.
- Recuperação automática limitada a uma tentativa a cada 3 minutos, somente depois de uma conexão WhatsApp confirmada.

## Preservação
- Não executa limpeza ou redefinição de `auth/`, `.env`, banco SQLite ou arquivos do projeto.
- Usa o `start-bg.sh` original, que exige `auth/creds.json` e não faz novo pareamento.
- Para parar, usa o `stop.sh` original, que preserva a sessão WhatsApp.

## Configuração obrigatória do dispositivo
O Termux exige `allow-external-apps=true` em `~/.termux/termux.properties`, além da permissão Android `com.termux.permission.RUN_COMMAND`. O Nicolas Keep Alive mostra e copia o comando de configuração, mas o próprio usuário deve executá-lo no Termux, conforme a proteção de segurança do Termux.

## Validação
A integração foi adaptada ao conteúdo do ZIP Robo Nicolas v8. A compilação e testes Android devem ser confirmados pela nova execução do GitHub Actions; o funcionamento real do bot e o estado WhatsApp dependem da execução no SM-A075M com a pasta privada original do Termux.
