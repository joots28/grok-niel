# Copy — status real e fila de conclusão

Atualizado em 2026-10-23. Fonte de trabalho: branch `copy/foundation-requirements` em `joots28/grok-niel`.

Este documento diferencia o que está implementado, o que apenas compila e o que foi testado em dispositivo. Uma tela ou uma execução de build não é evidência de que uma função do aparelho ou uma integração de IA esteja funcionando.

## Estado de entrega

- A PR #1 segue em rascunho; nada foi mesclado nem publicado em produção.
- O projeto Android nativo fica em `android/`, separado do workspace Rust original.
- CI verde no commit de código `17fdd4650c01aa7a1423477348efd526dfe9a710` (run #62): `assembleDebug`, `testDebugUnitTest`, `connectedDebugAndroidTest` e upload do APK de depuração passaram. O APK atualizado inclui exportação JSON e exclusão integral de memórias. Artefato GitHub Actions disponível até `2026-10-30 21:43 UTC`.
- Um build de CI ainda não equivale a um teste de instalação, execução, persistência após reiniciar ou uso em um celular real.

## O que já existe na base Android

- Projeto Kotlin + Jetpack Compose e navegação entre Início, Comandos, Controle, Memória e Histórico.
- Conversa local explicitamente identificada como demonstração; nenhuma IA está conectada.
- Catálogo de comandos com busca/filtros; nenhum comando controla o aparelho.
- A tela Início e o Histórico consultam sessões e mensagens reais do SQLite local; o Histórico pesquisa e permite excluir conversas com confirmação.
- A tela Memória usa SQLite local para salvar, listar, editar e excluir itens após confirmação; agora também exporta JSON por seletor de arquivos do Android e oferece exclusão integral após confirmação. Operações de banco/arquivo ocorrem fora da thread principal.
- Os testes unitários e instrumentados passaram no run #62: validam serialização JSON/escape, exportação vazia, recuperação de conversas após reabrir o banco, exclusão em cascata e CRUD/exclusão total de memória. Isso não substitui teste de ciclo de vida completo da UI nem teste físico.
- O manifesto usa `allowBackup=false` e o aplicativo não solicita permissões de microfone, calendário, localização ou acessibilidade nesta etapa.

## Lacunas que impedem chamar o Copy de pronto

### P0 — persistência local e segurança
1. [Concluído no CI] Compilação debug, testes unitários e instrumentados passaram no commit de código `17fdd4650c01aa7a1423477348efd526dfe9a710` (run #62).
2. [Pendente] Instalar e testar o APK num aparelho físico, incluindo navegação e retomada após encerrar/reabrir o aplicativo.
3. [Pendente] Definir e testar migrações antes de qualquer mudança de esquema SQLite.
4. [Concluído para memórias] Exportação JSON pelo seletor de arquivos e exclusão total mediante confirmação. [Pendente] Importação e exportação/exclusão integral de conversas.
5. [Pendente] Revisar proteção criptográfica para dados sensíveis. Os bancos SQLite atuais são privados ao app, mas **não possuem criptografia própria**.

### P1 — dados de conversa e histórico reais
1. [Validado em CI] Conversas e mensagens locais com ordenação e datas.
2. [Validado em CI] Histórico e conversas recentes consultam sessões reais, sem registros fictícios.
3. [Validado em CI] Exclusão individual de conversa com confirmação; anexos ainda não estão no escopo.
4. Testar migrações, múltiplas conversas, textos longos, estado vazio e falhas de armazenamento.

### P2 — contrato real de IA
1. Definir interfaces independentes de provedor, sessão, mensagem, resultado, erro, streaming e cancelamento.
2. Não enviar chamadas HTTP diretamente das telas Compose.
3. Tratar timeout, falta de rede, respostas malformadas, limite de uso e cancelamento.
4. Não embutir chaves de API no APK, no repositório nem nos logs; credenciais precisam de armazenamento protegido e configuração explícita do usuário.
5. Confirmar qual adaptador será conectado primeiro antes de escolher SDK ou formato proprietário.

### P3 — fronteira do runtime Rust
1. O repositório original é um agente de programação CLI/TUI, não um SDK Android pronto. A autenticação documentada inclui fluxos do produto upstream e arquivos locais próprios.
2. Inspecionar dependências e direitos de redistribuição de cada módulo que se pretenda reutilizar.
3. Comparar um serviço local com protocolo versionado, uma API separada e FFI (JNI/UniFFI) usando critérios de segurança, ciclo de vida, cancelamento, bateria e manutenção.
4. Provar a opção escolhida com um caso mínimo e testes antes de integrá-la à conversa.
5. Não reutilizar tokens da CLI no aplicativo nem supor que o login upstream funciona como credencial do Copy.

### P4 — voz e integração do Android
1. Captura de voz com permissão de microfone solicitada somente quando a pessoa ativa a função.
2. Definir reconhecimento de fala e síntese de voz, dependências de rede e comportamento offline.
3. Implementar integrações uma por vez; cada recurso deve declarar permissões, risco, resultado verificável e caminho de falha.
4. Colocar confirmação para ações sensíveis/destrutivas e manter trilha de auditoria sem conteúdo secreto.
5. Não expor controles falsos de sistema como se alterassem Wi‑Fi, Bluetooth, volume ou outros parâmetros.

### P5 — design, qualidade e distribuição
1. Extrair e comparar o protótipo Figma Make de forma fiel; o código/medidas completos ainda não foram extraídos, então a correspondência visual não está comprovada.
2. Testar acessibilidade, escala de texto, teclado, rolagem, estados de erro e telas pequenas.
3. Testar a faixa de versões Android suportada, desempenho, bateria, rede instável e retomada após interrupções.
4. Gerar APK assinado apenas quando os critérios de segurança e teste forem aceitos; guardar segredos de assinatura fora do Git.
5. Fazer revisão final de licenças e avisos de terceiros.

## Portões de qualidade para cada entrega

Uma etapa não é considerada concluída até haver:
- diff pequeno e revisado na branch de trabalho;
- resultado de build e testes registrado para o commit exato;
- teste de comportamento relevante, não só compilação;
- interface honesta sobre o que está ou não conectado;
- nenhum segredo no código ou nos logs;
- nenhum merge ou deploy sem autorização separada.

## Limites atuais

O histórico/conversas e a memória já estão conectados a SQLite no commit de código validado pelo CI. Ainda faltam teste em aparelho físico, proteção criptográfica do banco, integração real de IA, reconhecimento/síntese de voz, sincronização em nuvem, controles funcionais do Android e comprovação de fidelidade pixel a pixel com o Figma. O APK entregue é de depuração, não um lançamento assinado para produção.
