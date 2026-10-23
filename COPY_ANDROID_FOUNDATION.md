# Copy — fundação Android e contratos de integração

**Status:** build debug, testes unitários e testes instrumentados aprovados pelo GitHub Actions no commit de código `47bf8c456d8f8aea8dda484c898fb06f60e33814` (run #57). Teste em aparelho físico ainda pendente.  
**Base de trabalho:** `joots28/grok-niel`, branch `copy/foundation-requirements`.  
**Referência de requisitos:** Mapa Mestre do Copy, versão de planejamento de 22/10/2026; `COPY_AUDIT.md` e `COPY_ROADMAP.md`.

## Objetivo desta etapa

Converter as decisões aprovadas em uma base revisável e isolada para o cliente Android. A interface está em `android/`; conversas, histórico e memória usam SQLite local no código. O GitHub Actions passou em `assembleDebug`, testes unitários e instrumentados no commit de código `47bf8c4` (run #57). A validação no CI não substitui teste em aparelho físico.

## Decisões consolidadas

1. Android nativo em Kotlin como cliente principal.
2. Reaproveitamento seletivo do runtime Rust existente, condicionado à compatibilidade e à revisão dos módulos.
3. Comparar serviço local com API versionada, JNI e UniFFI; não escolher uma ponte Kotlin–Rust antes de avaliar ciclo de vida, bateria, isolamento, cancelamento e manutenção.
4. SQLite local, migrações versionadas, separação entre histórico e memória de longo prazo e operações de exportação/exclusão.
5. IA híbrida, com provedores opcionais por interfaces independentes; recursos remotos devem indicar requisitos de rede e custo.
6. Memória de longo prazo desativável e sujeita a confirmação, com consulta, edição, exportação e exclusão.
7. Camada de segurança independente do modelo: permissões mínimas, autorização baseada em risco, confirmação para ações sensíveis e trilha de auditoria sem dados secretos.
8. Credenciais fora do código-fonte, APK, repositório e logs. Avaliar Android Keystore para proteger material criptográfico; não presumir que ele protege automaticamente o banco inteiro.
9. Prioridade à capacidade offline viável, sem prometer paridade com provedores de nuvem nem gratuidade permanente de serviços externos.
10. Preservar licenças e avisos da base importada, incluindo `LICENSE` e `THIRD-PARTY-NOTICES`.

## Mapa da interface nativa

A implementação em `android/app/src/main/java/com/otiniel/copy/` usa Kotlin e Jetpack Compose para organizar as áreas principais do protótipo. Conversas/mensagens e memória usam bancos SQLite privados ao app, ainda sem criptografia própria. Build debug, testes unitários e testes instrumentados passaram no run #57; a reprodução pixel a pixel do Figma e o teste em aparelho físico seguem sem comprovação.

- **Início:** saudação, ações rápidas, conversas recentes, entrada de texto e atalho de voz.
- **Conversa:** mensagens, estado de processamento, cancelar, copiar e estados de erro.
- **Comandos:** busca, filtros por categoria e etiquetas indicando se cada recurso é demonstração, depende de internet ou exige permissão Android.
- **Central de controle:** atalhos e status de integrações. Não afirmar que Wi‑Fi, Bluetooth, volume ou outros parâmetros foram alterados sem confirmar uma operação Android real.
- **Memória:** listar, adicionar, confirmar, editar, excluir e pausar o uso da memória.
- **Histórico:** sessões pesquisáveis, criação de conversa e exclusão conforme política definida.
- **Configurações e integrações:** tema, preferências de voz, privacidade, permissões e dependências de conexão.

A inspeção do Figma Make confirmou uma interface baseada em React/Vite/Tailwind no protótipo e código visível no painel Arquivos. A extração do código completo e das medidas exatas de cada componente ainda não foi concluída; não presumir correspondência pixel a pixel.

## Contratos a definir antes de conectar a UI

### Interface de conversa
- Entrada: identificador de sessão, texto, anexos autorizados e opções de resposta.
- Saída: texto parcial/final, estado, referência de provedor quando aplicável e erro tipado.
- Controle: cancelamento, timeout, limite de tentativas e prevenção de ações duplicadas.
- Privacidade: não gravar conteúdo sensível em logs técnicos.

### Interface de ferramenta
- Cada ferramenta informa permissões requeridas, nível de risco e se exige confirmação.
- A política de segurança valida e autoriza a ação antes da execução; uma resposta do modelo nunca equivale à permissão.
- A execução deve produzir um resultado verificável, inclusive falha ou recusa.
- Operações externas e destrutivas exigem autorização explícita separada.

### Persistência local
- [Validado em CI/emulador] Tabelas SQLite separadas para sessões/mensagens e memórias, com versão de esquema inicial; testes instrumentados passaram no run #57.
- [Validado em CI/emulador] Listagem e exclusão de sessões com remoção de mensagens por chave estrangeira e CRUD de memória testado, inclusive reabertura do banco/repositório.
- [Pendente] Exportação/importação segura e exclusão integral sob confirmação.
- [Pendente] Revisão de migrações antes de qualquer alteração futura de esquema.
- [Pendente] Proteção criptográfica do conteúdo sensível; o banco não é criptografado nesta revisão.

## Gates de implementação

### Gate A — auditoria e ambiente
- Confirmar toolchain Rust 1.94.0 e requisitos DotSlash/protoc.
- Executar `cargo fmt --all -- --check` e verificações/testes direcionados.
- Registrar separadamente bloqueios de ambiente e falhas de código.
- `COPY_AUDIT.md` informa que build e testes ainda não foram executados; por isso a Fase 0 permanece aberta.

### Gate B — fundação Android
- [Parcial] Módulo Android Kotlin/Compose criado dentro de `android/`, com telas e dados de demonstração separados; domínio/integrações ainda precisam de módulos próprios.
- [Pendente] Contratos de erros, cancelamento, timeout e autenticação local.
- [Implementado, não validado] Navegação inicial entre Início, Comandos, Controle, Memória, Histórico e uma conversa de demonstração.
- [Validado em CI] `assembleDebug`, `testDebugUnitTest`, `connectedDebugAndroidTest` e upload do artefato passaram no run #57. O APK é de depuração; a instalação e execução em aparelho físico seguem pendentes.

### Gate C — integração Rust
- Comparação documentada entre serviço local, JNI e UniFFI.
- Adaptador mínimo com protocolo versionado.
- Testes de timeout, cancelamento, fechamento do app, indisponibilidade e dados malformados.
- Não transportar tokens do provedor para a camada de interface.

### Gate D — memória e segurança
- [Validado em CI/emulador] SQLite inicial para conversas/mensagens e memórias, com operações de salvar/ler/editar/excluir.
- [Validado em CI/emulador] Confirmação antes de salvar/excluir memória; testes instrumentados para reabertura do banco e operações CRUD passaram no run #57.
- [Pendente] Exportação/importação, exclusão integral, proteção criptográfica revisada e testes negativos de permissões/ferramentas.

## Fora do escopo desta etapa

Não publicar em produção, não alterar `joots28/grok-niel-build`, não remover fluxos da base upstream sem entender seus consumidores e não ativar integrações perigosas por padrão. O APK debug foi gerado e testado no CI (run #57); isso não equivale a um lançamento assinado nem a teste em dispositivo físico.

## Critério de aceite deste documento

Este arquivo organiza requisitos e critérios de validação. Ele não comprova que o aplicativo Android, a ponte Rust, SQLite, voz ou integrações do aparelho já foram implementados.
