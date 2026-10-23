# Copy — fundação Android e contratos de integração

**Status:** scaffold Android Kotlin/Jetpack Compose adicionado na branch de trabalho; compilação e validação em dispositivo ainda pendentes.  
**Base de trabalho:** `joots28/grok-niel`, branch `copy/foundation-requirements`.  
**Referência de requisitos:** Mapa Mestre do Copy, versão de planejamento de 22/10/2026; `COPY_AUDIT.md` e `COPY_ROADMAP.md`.

## Objetivo desta etapa

Converter as decisões aprovadas em uma base revisável e isolada para o cliente Android. A primeira interface navegável foi adicionada em `android/`; as telas não afirmam que IA, persistência, voz ou integrações do aparelho já estejam operacionais.

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

A primeira implementação em `android/app/src/main/java/com/otiniel/copy/` usa Kotlin e Jetpack Compose para organizar as cinco áreas principais do protótipo. Ela é uma fundação funcional de navegação, não uma reprodução pixel a pixel: a extração visual do Figma Make não foi concluída e esta versão ainda não foi compilada.

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
- Versão de esquema e migrações testadas.
- Histórico e memória de longo prazo em domínios separados.
- Operações explícitas de listar, editar, exportar e apagar.
- Backup/restauração com validação e sem incluir credenciais em exportações comuns.

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
- [Pendente] Build Android e testes de UI/domínio. O workflow de CI foi adicionado para tentar gerar e anexar um APK debug.

### Gate C — integração Rust
- Comparação documentada entre serviço local, JNI e UniFFI.
- Adaptador mínimo com protocolo versionado.
- Testes de timeout, cancelamento, fechamento do app, indisponibilidade e dados malformados.
- Não transportar tokens do provedor para a camada de interface.

### Gate D — memória e segurança
- SQLite com migração inicial e testes de persistência.
- Confirmação da memória, exportação e exclusão.
- Testes negativos para ferramentas sem autorização e para permissões negadas.

## Fora do escopo desta etapa

Não publicar em produção, não alterar `joots28/grok-niel-build`, não remover fluxos da base upstream sem entender seus consumidores, não ativar integrações perigosas por padrão e não afirmar que há APK ou build Android validado até que isso tenha sido realmente gerado e testado.

## Critério de aceite deste documento

Este arquivo organiza requisitos e critérios de validação. Ele não comprova que o aplicativo Android, a ponte Rust, SQLite, voz ou integrações do aparelho já foram implementados.
