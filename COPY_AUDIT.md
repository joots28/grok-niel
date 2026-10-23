# Copy — Auditoria inicial do repositório

Data da auditoria: 2026-10-23  
Branch inspecionada: `copy-foundation`  
Repositório: `joots28/grok-niel`

## Escopo e evidências consultadas

Foram lidos os arquivos abaixo diretamente na branch indicada:

- `COPY_ROADMAP.md`
- `README.md`
- `Cargo.toml`
- `rust-toolchain.toml`
- `SOURCE_REV`
- `crates/codegen/xai-grok-config/Cargo.toml`
- `crates/codegen/xai-grok-config/src/lib.rs`
- `crates/codegen/xai-grok-shell/Cargo.toml`
- `crates/codegen/xai-grok-shell/src/lib.rs`
- `crates/codegen/xai-grok-pager-bin/Cargo.toml`
- `crates/codegen/xai-grok-pager-bin/src/main.rs`
- `crates/codegen/xai-grok-pager/docs/user-guide/02-authentication.md`
- `LICENSE`
- `THIRD-PARTY-NOTICES`

## Constatações verificadas

1. **A base atual é um aplicativo Rust de terminal (CLI/TUI), não um aplicativo Android.** O README descreve um agente de programação para terminal e identifica `xai-grok-pager-bin` como ponto de entrada.
2. **O workspace é grande e usa Rust 2024.** O arquivo `rust-toolchain.toml` fixa Rust `1.94.0`; o README pede DotSlash e `protoc` para build.
3. **A configuração existente é complexa e não deve ser substituída às cegas.** `xai-grok-config` tem camadas de configuração, endpoints, requisitos gerenciados, compatibilidade e integração com configurações remotas.
4. **O runtime já é um crate separado.** `xai-grok-shell` contém módulos para agente, configuração, autenticação, sessões, ferramentas e integrações; deve ser reaproveitado seletivamente, não copiado inteiro para Android sem uma fronteira de integração.
5. **A autenticação documentada hoje é orientada ao produto upstream.** O guia descreve login via navegador e credenciais em `~/.grok/auth.json`, além de `XAI_API_KEY`. Isso não equivale a autenticação própria do Copy nem a um backend local.
6. **O projeto não pode ser descrito como pronto para celular ou como serviço gratuito.** O repositório consultado não comprova um cliente Android, uma API pública própria ou disponibilidade gratuita de provedores.
7. **Os avisos de licença fazem parte da entrega.** A raiz inclui `LICENSE` Apache-2.0 e `THIRD-PARTY-NOTICES`; mudanças e redistribuição devem preservar os avisos aplicáveis e revisar obrigações de componentes de terceiros.
8. **O workspace raiz se declara gerado.** O próprio `Cargo.toml` recomenda editar manifests por crate, não alterar a lista gerada de membros sem necessidade.

## Limites desta auditoria

- Os arquivos foram inspecionados pela interface do GitHub.
- **Não foi executado** `cargo fmt`, `cargo check`, `cargo test` ou build neste ambiente. Portanto, não há resultado de compilação ou testes a declarar.
- A inspeção de arquivos não substitui uma revisão de segurança completa nem confirma que todas as dependências podem ser compiladas em cada sistema operacional.

## Próximos passos recomendados

1. Fazer uma validação reproduzível do Rust em um ambiente de build, começando por `cargo fmt --all -- --check` e depois por verificações direcionadas aos crates alterados.
2. Definir uma fronteira mínima e explícita entre o runtime Rust e uma futura interface Android Kotlin. Antes de implementar FFI ou um serviço local, comparar custo, suporte multiplataforma e superfície de segurança.
3. Separar a identidade/configuração do Copy das suposições de produto upstream, sem remover autenticação ou fluxos existentes antes de entender seus consumidores.
4. Definir armazenamento local e memória com consentimento, exportação e exclusão, sem armazenar tokens em logs ou arquivos compartilhados.
5. Só então implementar a primeira fatia vertical pequena, com testes e diff revisável.
6. Não publicar em produção nem expor credenciais sem autorização separada.

## Status

**Auditoria documental inicial concluída; validação de build pendente.** Este documento registra achados e limites, não afirma que o Copy já foi implementado.
