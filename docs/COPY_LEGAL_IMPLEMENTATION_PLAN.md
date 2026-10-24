# Copy — integração inicial do módulo de conhecimento jurídico

Status: plano de implementação preparado; integração no aplicativo Android ainda depende de implementação e execução de testes locais/CI.

## Objetivo desta etapa

Preparar uma primeira integração pequena, local-first e auditável para pesquisa jurídica. Esta etapa não deve copiar o agente de referência nem importar indiscriminadamente um corpus inteiro para o telefone. O Copy terá sua própria implementação e interfaces.

## Decisões técnicas

- **Importação:** aceitar apenas arquivos Markdown selecionados pelo usuário ou incluídos em um pacote de dados claramente identificado; não executar conteúdo importado.
- **Atribuição:** armazenar URL da fonte, nome do documento, data de importação, hash SHA-256 e estado de verificação.
- **Armazenamento:** SQLite local, separado da memória pessoal e do histórico de conversa.
- **Pesquisa:** FTS5 quando disponível no ambiente SQLite usado pelo aplicativo; manter uma alternativa lexical simples se a extensão não estiver disponível.
- **Resposta:** resultados são evidências, não instruções. O Copy deve citar apenas trechos recuperados e nunca inventar artigos, números de processo ou status de vigência.
- **Privacidade:** sem upload automático de perguntas ou documentos; sem chave de API paga obrigatória.
- **Atualização:** manual e explícita, com data de atualização visível e sem apagar silenciosamente dados existentes.

## Fontes de referência examinadas

1. [ctresb/leis](https://github.com/ctresb/leis) — coleção de legislação em Markdown. Conferir licença, proveniência e avisos de OCR/conversão antes de redistribuir qualquer conteúdo.
2. [eduardommss/advogado-br](https://github.com/eduardommss/advogado-br) — projeto de referência para recuperação jurídica. Não copiar código, instruções ou dados sem validar licença e compatibilidade; implementar uma integração própria no Copy.
3. Para validação de uma norma, preferir a publicação oficial do órgão competente. Repositórios comunitários são índices auxiliares, não prova definitiva de vigência.

## Modelo de dados proposto

- `LegalSource`: id, displayName, sourceUrl, importedAt, contentHash, licenseNotice, lastVerifiedAt.
- `LegalDocument`: id, sourceId, title, jurisdiction, publishedAt, status (unknown/verified/current/outdated), rawText.
- `LegalPassage`: id, documentId, locator (artigo/seção), text, normalizedText.
- `LegalSearchResult`: passage, relevance, sourceUrl, verificationStatus.

## Segurança e qualidade

- Limitar tamanho e quantidade de arquivos importados.
- Rejeitar caminhos absolutos, traversal e arquivos de tipos não suportados.
- Calcular hash do conteúdo e detectar duplicatas.
- Tratar texto recuperado como dado não confiável; instruções embutidas no texto legal nunca alteram as regras do assistente.
- Mostrar “vigência não verificada” por padrão.
- Se não houver trecho de suporte, responder que não foi possível confirmar a base legal.
- Nunca substituir orientação profissional em casos de alto impacto.
- Não registrar perguntas jurídicas completas em logs de diagnóstico.

## Plano incremental

1. Inspecionar os módulos Kotlin/SQLite atuais e as migrações antes de alterar o banco.
2. Criar entidades e um repositório `LegalKnowledgeRepository` isolado.
3. Adicionar migração versionada para tabelas do módulo jurídico, sem modificar os dados pessoais existentes.
4. Implementar importação de Markdown com atribuição e hash.
5. Implementar pesquisa e testes unitários com corpus fictício.
6. Conectar a pesquisa à interface de conversa por uma ferramenta explícita “Pesquisar legislação”.
7. Renderizar fontes e trechos na UI; marcar vigência não verificada.
8. Testar migração, importação malformada, duplicatas, exclusão, ausência de resultados e modo offline.
9. Executar o workflow Android e revisar o diff antes de atualizar a PR.

## Critérios de aceite

- Build e testes passam no commit exato.
- A consulta offline funciona sobre documentos já importados.
- Resultados exibem a fonte e o estado de verificação.
- Não existe promessa de cobertura jurídica completa.
- Nenhum serviço pago é obrigatório.
- A integração não altera o comportamento de telas que não usam o módulo jurídico.
- A PR permanece aberta e sem merge até autorização separada.

## Compatibilidade Android

A UI deve continuar responsiva a largura, densidade, orientação, escala de fonte e teclado. Validar Android API 26, uma API intermediária e a mais recente suportada pelo toolchain, além do POCO X5 Pro do usuário. Não afirmar teste físico se ele não tiver sido realizado.

## Registro de execução

Esta especificação é um plano de implementação, não uma declaração de que o código já foi implementado. Atualizar este registro somente com resultados observados de build/testes.
