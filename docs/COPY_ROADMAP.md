# Copy — Plano de evolução

Copy é o assistente pessoal Android-first, com núcleo local e conectado, memória controlável e ferramentas autorizadas. Este documento separa capacidades existentes na base de funcionalidades que ainda precisam ser implementadas ou validadas.

## Estado atual — 25 de outubro de 2026

- Repositório: `joots28/grok-niel`; branch de trabalho: `copy-foundation`.
- Foi adicionada uma primeira fundação Android independente em `android/`, usando Kotlin e Jetpack Compose.
- A fundação contém interface de conversa provisória, histórico SQLite local, comandos limitados de hora/data/ajuda, captura de fala por serviço disponível no Android e leitura em voz alta por TextToSpeech.
- Foram adicionados testes unitários dos comandos locais e um workflow de CI para executar testes e montar o APK de debug.
- O núcleo Rust ainda é um agente de terminal; não há integração Kotlin–Rust nem motor de IA conectado à tela.
- A interface não foi comparada com o Figma específico do Copy; não se afirma correspondência visual exata.
- Build, testes automatizados e instalação no POCO X5 Pro ainda precisam de resultados observados antes de marcar a fase como validada.

## Princípios

- Reaproveitar a arquitetura Rust existente depois de mapear interfaces e dependências.
- Criar identidade, configuração e experiência próprias para o Copy; não apresentar o projeto como produto oficial da xAI.
- Preservar `LICENSE`, `THIRD-PARTY-NOTICES` e os avisos de licenças de dependências.
- Nunca colocar chaves de API, tokens ou credenciais no repositório.
- Exigir autorização explícita para operações externas ou destrutivas; manter logs úteis sem registrar segredos.
- Fazer mudanças pequenas, revisar os arquivos envolvidos e executar verificações direcionadas antes de integrar.

## Estado inicial verificado

- Base: workspace Rust com edição 2024 e múltiplos crates.
- Binário de entrada atual: `xai-grok-pager-bin`; a base original é um agente de programação de terminal (TUI/CLI).
- Runtime do agente: crate `xai-grok-shell`.
- Configuração: crate `xai-grok-config`.
- O arquivo raiz `Cargo.toml` é gerado e deve ser tratado como somente leitura; preferir manifests por crate.
- A compilação/testes do workspace ainda precisam ser executados em ambiente de build reproduzível.

## Fases propostas

### Fase 0 — Diagnóstico reproduzível
1. Conferir toolchain Rust, dependências de build e instruções.
2. Executar `cargo fmt --all -- --check` e verificações/testes de crates específicos.
3. Registrar erros de ambiente separadamente de erros de código.
4. Mapear autenticação, seleção de modelo, configuração e limites de permissões.

**Estado:** auditoria documental inicial registrada; compilação e testes Rust não executados nesta sessão.

### Fase 1 — Fundação Android
1. Configurar a aplicação Kotlin e a tela de conversa.
2. Adicionar persistência local e uma primeira bateria de testes.
3. Configurar CI para build do APK de debug.

**Estado:** arquivos criados na branch `copy-foundation`; aguardando resultado do workflow e teste em dispositivo.

### Fase 2 — Design fiel e contratos
1. Identificar a URL/arquivo correto do Figma do Copy e capturar a referência visual autorizada.
2. Ajustar a interface para corresponder ao design aprovado, sem redesenho arbitrário.
3. Definir contrato versionado, modelo de erros, timeout e cancelamento para a integração Kotlin–Rust.

**Portão:** comparar a tela com o Figma e testar o contrato sem executar ações externas não autorizadas.

### Fase 3 — Núcleo Rust e fluxo vertical
1. Inspecionar os pontos de extensão do runtime sem remover fluxos existentes.
2. Comparar serviço local, JNI e UniFFI para a fronteira Kotlin–Rust.
3. Implementar uma solicitação de ponta a ponta e validar cancelamento, timeout, erros e isolamento.

**Portão:** compilação e testes reproduzíveis, com logs sem segredos.

### Fase 4 — Memória e segurança
1. Ampliar SQLite com migrações, memória opt-in, consulta/edição/exportação/exclusão.
2. Avaliar Android Keystore para segredos e proteção adequada dos dados.
3. Adicionar políticas de permissões e auditoria independente da saída do modelo.

**Portão:** persistência, recuperação, exportação e bloqueio de ações não autorizadas testados.

### Fase 5 — IA híbrida, voz e ferramentas
1. Introduzir interfaces independentes de provedores e seleção configurável.
2. Avaliar modelo local e serviço de nuvem opcional; não assumir gratuidade permanente.
3. Integrar voz e ferramentas Android de forma gradual, por permissões mínimas.

**Portão:** falhas de rede, limites, privacidade e comportamento offline demonstrados.

### Fase 6 — Robustez e release controlado
1. Regressão, acessibilidade, bateria, memória, atualização, backup e restauração.
2. Instalar e testar no POCO X5 Pro, além de validar telas menores/maiores.
3. Preparar APK, instruções, versão e estratégia de reversão.

**Portão:** aprovação do checklist de qualidade. Publicação externa exige autorização específica.

## Critérios de conclusão de cada etapa

- Diff pequeno e revisável.
- Testes executados com resultados registrados; testes não executados devem ser declarados.
- Nenhum segredo adicionado aos commits.
- Nenhuma publicação em produção sem autorização separada.
- Licenças e avisos de terceiros preservados.
- Interface declarada como fiel ao Figma apenas após comparação verificável.

## Custos e dependências

O núcleo deve funcionar sem serviço próprio obrigatório. Provedores, sincronização e recursos remotos serão opcionais, com custos/limites documentados. A pesquisa via Exa pode apoiar desenvolvimento e extração web, mas não substitui o runtime do assistente nem constitui automaticamente automação universal de navegador.

## Próxima ação

Validar o workflow de build/teste da fundação Android. Em paralelo, identificar o Figma correto do Copy e mapear o crate de runtime para escolher uma fronteira segura de integração Kotlin–Rust. Não tratar planejamento ou arquivos criados como prova de que o produto completo está implementado.
