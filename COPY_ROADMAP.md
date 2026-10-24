# Copy — roadmap de implementação

## Objetivo

Desenvolver o Copy, com identidade de assistente Luo, como uma experiência Android local-first inspirada em um Mini-Jarvis: conversa, voz, memória, pesquisa e ferramentas, com automação controlada e custos obrigatórios iguais a zero.

A arquitetura híbrida permite serviços online gratuitos opcionais, mas nenhum provedor deve ser considerado permanentemente gratuito, ilimitado ou privado sem verificação.

## Estado confirmado

- Cliente Android nativo em Kotlin + Jetpack Compose dentro de android/, separado do workspace Rust upstream.
- Histórico, conversas e memória locais; conteúdo de conversas/memórias usa cifra de campos AES-GCM, não criptografia integral dos bancos SQLite.
- Modelo LiteRT-LM opcional importado pelo usuário; geração local ainda precisa de teste real no POCO X5 Pro.
- Entrada por botão com reconhecimento local quando suportado e leitura de respostas por TTS manual. Palavra de ativação contínua não está implementada.
- Memórias têm controles de edição/exclusão/exportação. A mudança no commit 1d16ec3c660ff51f0074a7d0a126b5fd64df6a38 adiciona exportação JSON e exclusão total do histórico; o CI desse commit ainda estava pendente na última consulta.
- PR #1 continua em rascunho. Não houve merge, deploy de produção ou release assinada.

## Próximas etapas — ordem recomendada

### P0 — Verificação da entrega atual

1. Acompanhar a execução do CI do commit 1d16ec3c e corrigir qualquer falha de compilação/teste.
2. Publicar o link do APK de depuração correspondente ao commit validado, com data de expiração do artefato.
3. Registrar o que só pode ser validado no aparelho físico: instalação, histórico após reinício, reconhecimento e síntese de voz, inferência do modelo, RAM, temperatura e bateria.

### P1 — Privacidade e recuperação local

1. Revisar a cifra de campos, as migrações, as cópias temporárias e os metadados armazenados.
2. Desenhar importação de backup com validação estrita de esquema, limite de tamanho, transação e prevenção de duplicatas.
3. Avaliar bloqueio com BiometricPrompt/PIN e timeout de sessão sem bloquear indevidamente os fluxos de recuperação.
4. Separar permissões e controles por categoria; logs não devem guardar conteúdo sensível.

### P2 — Núcleo de IA verificável

1. Formalizar contratos de provedor/modelo, streaming, cancelamento, timeout e falhas.
2. Validar o runtime LiteRT-LM no dispositivo com um modelo real e registrar latência/memória antes de afirmar que a IA local está pronta.
3. Definir o limite de dados que pode sair do aparelho e solicitar aprovação específica antes de enviar contexto privado.
4. Não colocar chaves de API no APK nem depender de APIs pagas.

### P3 — Voz e agente

1. Melhorar entrada/saída de voz acionadas pelo usuário e testar idiomas/serviços instalados.
2. Avaliar palavra de ativação local só depois de medir consumo, permissões, confiabilidade e restrições de segundo plano da Xiaomi.
3. Implementar núcleo de tarefas com estados, dependências, cancelamento, verificação de resultado e proteção contra ações duplicadas.

### P4 — Ferramentas e integrações

Implementar uma por vez, com permissões mínimas e testes próprios: pesquisa web citável, calendário/tarefas, arquivos autorizados, notificações, comandos Android e acessibilidade. A interface deve indicar claramente o que está realmente conectado.

### P5 — Fidelidade visual e distribuição

1. Recuperar a referência verificável do Figma e comparar cada tela/estado antes de alterar o design.
2. Testar telas pequenas, escala de fonte, teclado, contraste, acessibilidade e navegação.
3. Preservar licenças e avisos da base Rust importada.
4. Preparar distribuição de depuração e, somente após autorização separada, fluxo seguro para release assinada.

## Critérios de conclusão

Cada incremento precisa de diff revisável, testes ligados ao commit, critérios de aceitação e limites documentados. Build verde não substitui teste em dispositivo real; controles visuais não substituem integração funcional. Merge, produção e lançamento estável exigem autorização separada.
