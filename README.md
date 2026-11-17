# Nicolas Keep Alive

Painel Android para gerenciar o Robô Nicolas já instalado em `~/Robo-Nicolas` no Termux. O APK não incorpora nem substitui o bot; envia ações controladas ao Termux e exibe os resultados.

## Configuração obrigatória do Termux

O Termux bloqueia comandos de apps externos por padrão. No Termux, execute uma única vez:

```bash
mkdir -p ~/.termux
touch ~/.termux/termux.properties
if grep -q '^[[:space:]]*allow-external-apps[[:space:]]*=' ~/.termux/termux.properties; then
  sed -i 's/^[[:space:]]*allow-external-apps[[:space:]]*=.*/allow-external-apps=true/' ~/.termux/termux.properties
else
  printf '\nallow-external-apps=true\n' >> ~/.termux/termux.properties
fi
termux-reload-settings
```

Depois, nas Configurações do Android:
1. Apps > Nicolas Keep Alive > Permissões.
2. Abra **Permissões adicionais**.
3. Autorize **Run commands in Termux environment**.
4. Volte ao app e toque em **ATUALIZAR STATUS**.

A integração usa o serviço oficial `com.termux.app.RunCommandService` e a ação `com.termux.RUN_COMMAND`. Documentação: https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent

## Comandos reais do Robô Nicolas

A ponte executa apenas scripts fixos no diretório `~/Robo-Nicolas`:

- Iniciar: `bash start-bg.sh`
- Parar: `bash stop.sh`
- Reiniciar: `bash stop.sh` e, depois, `bash start-bg.sh`
- Status: confere o PID `data/bot.pid`, o gateway `python3 -m nicolas health`, a sessão `auth/creds.json` e o log `logs/supervisor.log`.
- Logs: mostra as últimas linhas de `logs/supervisor.log` e `logs/bot.log`.

O `start-bg.sh` do projeto exige que a primeira autenticação tenha sido feita pelo procedimento normal `bash start.sh`. Como o ZIP do bot não inclui a pasta privada `auth/`, a sessão existente deve permanecer no próprio aparelho.

## Sessão WhatsApp existente

O app não apaga, move, renomeia, restaura nem substitui `auth/`, `.env`, SQLite ou arquivos do bot. Para iniciar em segundo plano, usa o `start-bg.sh` fornecido pelo próprio Robô Nicolas, que exige `auth/creds.json`. Se a sessão não existir ou o Baileys indicar autenticação inválida, o app não gera QR/código nem faz logout; informa que é necessária uma ação manual no Termux.

O estado **WhatsApp conectado** só é indicado quando a mensagem `Autenticado e conectado ao WhatsApp` no log é mais recente que as mensagens de reconexão/desconexão. É uma confirmação por log, não uma consulta direta ao WhatsApp.

## Monitoramento

O Foreground Service consulta o status no Termux a cada 60 segundos. A recuperação automática do supervisor é limitada a uma tentativa por intervalo de 3 minutos e só é ativada após uma conexão WhatsApp previamente confirmada. O próprio `start.sh` mantém a supervisão interna do gateway e da ponte Baileys.

Android/Samsung ainda podem encerrar processos por economia de bateria, falta de memória ou reinicialização. Remova restrições de bateria para Nicolas Keep Alive e Termux. Nenhum aplicativo pode prometer disponibilidade permanente.

## Build e testes

O GitHub Actions executa `gradle check` e `gradle assembleDebug`, publica o APK debug e o ZIP de fonte como artefatos. Nenhuma sessão, segredo, banco ou arquivo `.env` é enviado no repositório.
