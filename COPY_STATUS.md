# Copy — status real e fila de conclusão

Atualizado em 2026-10-23. Fonte de trabalho: branch `copy/foundation-requirements` em `joots28/grok-niel`.

Este documento diferencia o que está implementado, o que apenas compila e o que foi testado em dispositivo. Uma tela ou uma execução de build não é evidência de que uma função do aparelho ou uma integração de IA esteja funcionando.

## Estado de entrega

- A PR #1 segue em rascunho; nada foi mesclado nem publicado em produção.
- O projeto Android nativo fica em `android/`, separado do workspace Rust original.
- O CI passou no commit `f4f0ccb` (build debug, teste unitário disponível e upload do APK). A revisão de memória SQLite é posterior a esse commit e precisa do resultado de CI dessa nova revisão.
- Um build de CI ainda não equivale a um teste de instalação, execução, persistência após reiniciar ou uso em um celular real.

## O que já existe na base Android

- Projeto Kotlin + Jetpack Compose e navegação entre Início, Comandos, Controle, Memória e Histórico.
- Conversa local explicitamente identificada como demonstração; nenhuma IA está conectada.
- Catálogo de comandos com busca/filtros; nenhum comando controla o aparelho.
- Histórico ainda usa registros estáticos de exemplo.
- A tela de Memória está sendo convertida para SQLite local: salvar, listar, editar e excluir após confirmação, validação de campos e operações de banco fora da thread principal.
- Testes unitários de validação de memória foram adicionados; a execução do CI desta revisão ainda precisa ser consultada.
- O manifesto usa `allowBackup=false` e o aplicativo não solicita permissões de microfone, calendário, localização ou acessibilidade nesta etapa.

## Lacunas que impedem chamar o Copy de pronto

### P0 — validar a primeira fatia funcional
1. Confirmar CI verde no commit da implementação SQLite; corrigir qualquer falha real antes de ampliar o escopo.
2. Testar gravação, leitura, edição e exclusão em um emulador Android; depois testar em aparelho físico.
3. Testar ciclo de vida: sair da tela, fechar/reabrir o app e reiniciar o processo sem perder registros.
4. Incluir uma estratégia de migração antes de mudar o esquema SQLite.
5. Decidir e implementar exportação e exclusão completa da memória.
6. Criar uma solução de proteção dos dados sensíveis; o SQLite atual é privado ao app, mas **não possui criptografia própria**. Não descrever como criptografado.

### P1 — dados de conversa e histórico reais
1. Modelar conversas e mensagens em armazenamento local, com ordenação e datas reais.
2. Fazer o Histórico e a tela inicial consultarem sessões reais, sem dados fictícios.
3. Adicionar exclusão de conversa com confirmação e política clara para anexos.
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

Não há confirmação de teste físico, integração real de IA, reconhecimento/síntese de voz, sincronização em nuvem, controles do sistema Android, histórico persistente ou fidelidade pixel a pixel com o Figma. Esses itens permanecem pendentes mesmo se o próximo APK compilar.
