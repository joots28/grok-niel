# Copy — fidelidade visual e compatibilidade Android

## Prioridade visual

Referência principal: https://www.figma.com/make/lLBQRSKr57N7mXWc7a1ZXk?node-id=

O aplicativo nativo deve reproduzir a referência original, tela por tela, incluindo hierarquia, cores, tipografia, espaçamentos, formas, ícones, estados vazios/carregando/erro, animações e navegação. Não redesenhar por preferência própria nem substituir elementos por aproximações sem registrar a diferença.

**Limitação de validação atual:** a ferramenta de contexto do Figma bloqueou novas leituras por limite do plano Starter. Até que screenshots/código do protótipo estejam acessíveis novamente, a fidelidade pixel a pixel não pode ser declarada. A base Android existente é uma fundação funcional, não prova de correspondência exata.

## Comportamento adaptável

- Preservar a composição móvel original nas telas estreitas.
- Em janelas largas, centralizar o conteúdo e limitar a largura útil para evitar linhas excessivamente longas; não esticar os cartões por toda a tela.
- Respeitar barras do sistema, teclado, orientação, densidade e escala de fonte do Android.
- Preferir dimensões em dp e texto em sp; evitar posições absolutas quando não forem parte essencial da referência.
- Testar estados de rolagem, teclado aberto, textos longos, fontes ampliadas e telas pequenas.

A largura máxima inicial de 760 dp aplicada ao contêiner principal é uma regra de adaptação, não uma alegação de que o layout final já foi aprovado contra o Figma.

## Compatibilidade-alvo

- minSdk = 26: Android 8.0 ou posterior, sujeito à disponibilidade de cada API.
- Testes planejados: API 26, uma versão intermediária e a API de referência mais recente suportada pelo toolchain.
- Dispositivo físico prioritário: POCO X5 Pro 5G do usuário, 8 GB RAM / 256 GB, para conferir proporções, navegação, teclado, desempenho e consumo.
- Não assumir que microfone, acessibilidade, comandos do sistema ou execução em segundo plano se comportam igual em todas as versões e fabricantes. Cada integração deve verificar versão, permissão e resultado real.

## Direção funcional: assistente inspirado em JARVIS

A meta é um assistente cinematográfico e útil, não uma simulação de ficção:

1. Conversa local e histórico persistente.
2. Camada de IA desacoplada do provedor, com streaming, cancelamento, erros claros e escolha explícita de configuração.
3. Voz ativada pelo usuário, com permissão solicitada no momento de uso.
4. Registro de ferramentas com permissões mínimas, confirmação de ações sensíveis e resultado verificável.
5. Memória separada do histórico, com controles de consulta, edição, exportação e exclusão.
6. Estado visual honesto: não exibir uma integração como ativa quando ela ainda é demonstração.
7. Custo zero como prioridade: APIs nativas e bibliotecas gratuitas; nenhum serviço pago obrigatório, sem prometer que todo modelo remoto ou recurso de nuvem será gratuito para sempre.

## Portões antes de declarar concluído

- Obter uma referência visual verificável do Figma e comparar cada tela/estado.
- Executar build e testes para o commit exato.
- Validar layouts em emuladores com diferentes tamanhos/API e no POCO X5 Pro.
- Corrigir diferenças visuais e bugs de adaptação antes de avançar para polimento ou distribuição.
- Não mesclar nem publicar sem autorização separada.
