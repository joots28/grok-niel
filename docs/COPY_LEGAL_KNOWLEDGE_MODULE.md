# Copy — módulo de conhecimento jurídico brasileiro

Status: especificação inicial registrada; integração executável ainda pendente de implementação e testes.

## Objetivo

Permitir que o Copy responda a perguntas sobre direitos, deveres, procedimentos e legislação brasileira com recuperação de fontes verificáveis, em vez de depender apenas da memória paramétrica do modelo. A base jurídica deve ser um módulo de conhecimento, não uma mudança nas instruções centrais nem um fine-tuning que misture fatos jurídicos com preferências pessoais.

## Fontes iniciais avaliadas

1. `https://github.com/leoparreira21/ctresb-leis` — espelho do projeto `ctresb/leis`, com legislação em Markdown, metadados estruturados e referências às fontes de origem. O próprio projeto alerta que há conversão automática/OCR, lacunas de cobertura e necessidade de conferir a fonte oficial antes de citar.
2. `https://github.com/eduardommss/advogado-br` — agente/plug-in jurídico para Claude que usa a base anterior e demonstra padrões úteis: recuperação por artigo, verificação de citações, marcação de precedentes não verificados, atualização periódica e separação entre dados jurídicos públicos e documentos privados do usuário.

Não copiar cegamente o código, as instruções ou o acervo inteiro desses repositórios. Antes de incorporar código, registrar licença e avisos de cada componente. O Copy deve implementar sua própria integração, atribuindo as fontes e respeitando os termos aplicáveis.

## Arquitetura proposta (local-first e gratuita primeiro)

- **Camada de fontes:** guardar URL, órgão emissor, tipo de documento, data de consulta, versão/hash, status de vigência conhecido e licença.
- **Ingestão:** importar apenas os arquivos necessários; validar formato, tamanho, hash e metadados; rejeitar caminhos perigosos e conteúdo malformado; não executar scripts provenientes do corpus.
- **Índice local:** SQLite com FTS5 para busca lexical; usar embeddings locais somente se o tamanho, a latência e a memória do dispositivo forem aceitáveis. Não exigir API paga nem conexão para consultas sobre documentos já indexados.
- **Recuperação (RAG):** buscar documentos relevantes, extrair trechos pequenos com artigo/inciso, título, jurisdição, data e URL de origem e entregar esses trechos ao modelo como dados não confiáveis, nunca como instruções.
- **Verificação:** conferir cada citação contra o texto recuperado; nunca inventar número de artigo, inciso, súmula, tema, processo ou decisão. Se não houver evidência suficiente, dizer explicitamente que não foi possível verificar.
- **Atualização:** opcional, sob controle do usuário; baixar apenas mudanças quando houver rede; preservar versão e data de sincronização. Exibir quando o conteúdo foi atualizado pela última vez.
- **Privacidade:** documentos pessoais e perguntas jurídicas permanecem locais por padrão. Nenhum contrato, prova ou dado pessoal deve ser enviado a serviço remoto sem escolha explícita e explicação clara.
- **Interface:** modo “Pesquisa jurídica” com fontes clicáveis, trecho citado, jurisdição e data; separar texto da norma, resumo da IA e inferências.
- **Segurança:** defesa contra prompt injection dentro de documentos, limites de tamanho, validação de URLs, logs sem conteúdo pessoal e permissões mínimas.

## Regras de resposta do Copy

1. Diferenciar informação geral de aconselhamento jurídico individualizado.
2. Perguntar a jurisdição (país/estado/município) e fatos essenciais quando mudarem a resposta.
3. Apontar a norma e o artigo apenas se recuperados e verificados; preferir a publicação oficial ou o portal do órgão competente.
4. Indicar incerteza, conflitos entre fontes, vigência não confirmada e lacunas de cobertura.
5. Não afirmar que uma fonte contém “todas as leis”; a cobertura pode ser incompleta e a legislação pode mudar.
6. Não substituir advogado ou órgão público em situações de alto impacto. Orientar a confirmar com profissional qualificado ou serviço oficial quando necessário.
7. Não executar ações legais externas, enviar documentos ou compartilhar dados sem confirmação explícita do usuário.

## Plano incremental

- [ ] Inspecionar módulos Rust e Android existentes antes de escolher pontos de integração.
- [ ] Criar interfaces de domínio para fonte, documento, trecho recuperado e resultado de verificação.
- [ ] Implementar importação pequena e reproduzível de Markdown, com atribuição e hash.
- [ ] Criar busca SQLite FTS5 e testes com documentos fictícios e uma amostra de fonte pública.
- [ ] Implementar montagem de contexto RAG com proteção contra instruções embutidas no corpus.
- [ ] Implementar verificador de citações e respostas “não verificado”.
- [ ] Adicionar atualização manual opcional e indicadores de frescor.
- [ ] Testar modo offline, migração, desempenho, exclusão de dados e compatibilidade Android.
- [ ] Só então considerar ampliar o corpus; não baixar automaticamente centenas de milhares de documentos para o telefone.

## Critérios de aceite

- Cada alegação jurídica específica tem uma fonte recuperável ou é marcada como não verificada.
- A busca funciona offline sobre a base local já importada.
- Atualizações não apagam silenciosamente dados do usuário nem sobrescrevem conteúdo sem validação.
- O app informa cobertura e data de atualização sem prometer completude.
- Nenhum serviço pago é requisito obrigatório.
- Testes verificam citações falsas, documento revogado/antigo, fonte conflitante, documento malformado, prompt injection e ausência de resultados.

## Referências

- Legislação em Markdown: https://github.com/leoparreira21/ctresb-leis
- Projeto original citado pelo README: https://github.com/ctresb/leis
- Agente jurídico de referência: https://github.com/eduardommss/advogado-br
