# Correções do Nicolas Keep Alive

## Correções aplicadas nesta revisão
- Botões INICIAR, PARAR e REINICIAR enviam ações pré-definidas ao serviço oficial RUN_COMMAND do Termux.
- A aplicação recebe stdout, stderr, código de saída e erros internos do Termux via PendingIntent.
- A tela mostra separadamente processo do bot, estado WhatsApp e condição da integração Termux.
- Serviço em primeiro plano consulta o estado a cada 60 segundos.
- Reconexão automática limitada: só após uma conexão WhatsApp anteriormente confirmada e com intervalo mínimo de 3 minutos.
- A execução utiliza os scripts existentes `~/Robo-Nicolas/start.sh` e `stop.sh`.

## Proteção dos dados
- Não são executados comandos de limpeza de auth, QR, pareamento, banco, .env ou dados pessoais.
- A sessão Baileys já salva permanece no diretório original.
- O app só inicia o script existente. Se a sessão estiver inválida, um novo pareamento não é iniciado automaticamente.

## Pré-requisitos obrigatórios
- Termux 0.109 ou superior.
- `allow-external-apps=true` em `~/.termux/termux.properties`.
- Permissão Android `com.termux.permission.RUN_COMMAND` concedida ao app.
- Os scripts `start.sh` e `stop.sh` presentes em `~/Robo-Nicolas`.

## Limite de validação
A compilação Android pode ser validada pelo GitHub Actions. A execução real do processo e o estado WhatsApp precisam ser confirmados no aparelho, com a versão e sessão Baileys instaladas pelo usuário. O app reporta confirmação de conexão somente quando encontra a mensagem de autenticação no log capturado da execução.
