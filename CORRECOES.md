# CORRECOES

## 1. Foreground Service
Implementado serviço de primeiro plano com notificação persistente e WakeLock temporário.

## 2. Termux
A ponte foi isolada em `termux/`. Nenhum comando arbitrário de ZIP é executado.

## 3. Pareamento
Foi criado `PairingValidator` com SHA-256/HMAC. O APK não contém um código secreto fixo.

## 4. Dados
O projeto distribuído não contém `.env`, banco, sessão WhatsApp ou credenciais do Robô Nicolas.

## 5. Limitações conhecidas
- A implementação de importação SAF/backup completo e execução real do Robô Nicolas ainda precisa ser conectada ao layout e ao protocolo exato do bot existente.
- O serviço Android pode ser encerrado por políticas do fabricante.
- A ponte Termux depende da instalação/configuração do Termux e da extensão escolhida.

Essas limitações são intencionais: não foram mascaradas como funcionalidades já testadas.
