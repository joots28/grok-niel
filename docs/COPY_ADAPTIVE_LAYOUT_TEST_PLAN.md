# Copy — validação de layout adaptável

Este documento define a estratégia de validação responsiva do app nativo. Não substitui a comparação visual com o Figma nem declara testes executados.

## Princípios

- Decidir o layout pela largura/altura disponível da janela, não pelo modelo do aparelho.
- Preservar a hierarquia visual original em telas compactas; usar mais espaço em telas médias/expandidas sem esticar conteúdo indefinidamente.
- Respeitar insets do sistema, teclado, rotação, modo de tela dividida e alterações de tamanho em tempo de execução.
- Usar dp para dimensões e sp para texto, sem bloquear a escala de fonte do usuário.
- Estados de carregamento, erro, vazio e permissões devem ser legíveis e acessíveis.
- Não introduzir novos padrões visuais antes de comparar com a referência Figma disponível.

## Matriz de teste

| Classe | Cenários | Critérios |
|---|---|---|
| Compacta | telefone estreito; retrato; teclado aberto; fonte 1.3x e 1.5x | sem corte horizontal; botões utilizáveis; campo de texto acessível |
| Compacta baixa | telefone em paisagem | conteúdo rolável; nenhum controle essencial escondido pelo teclado |
| Média | tablet pequeno ou janela dividida | largura legível; navegação sem sobreposição |
| Expandida | tablet / foldable aberto | conteúdo centralizado; panes extras apenas se coerentes com o Figma |
| Mudança dinâmica | rotação, split-screen, redimensionamento | estado da conversa preservado; sem reinicialização inesperada |
| Acessibilidade | TalkBack, foco, contraste, escala de fonte | rótulos acessíveis; ordem de foco previsível; conteúdo não truncado |

## Dispositivo prioritário

POCO X5 Pro 5G (8 GB RAM / 256 GB) é um alvo físico prioritário. Não marcar teste físico como concluído sem instalar e operar o APK no dispositivo.

## Base Android

O módulo declara minSdk 26 (Android 8.0). Isso significa compatibilidade mínima configurada, não garantia de funcionamento de cada recurso. Recursos de voz, notificações, acessibilidade, comandos do sistema e segundo plano exigem testes separados por versão, permissões e fabricante.

## Referência técnica

As recomendações seguem a documentação oficial do Android para layouts adaptáveis e classes de tamanho de janela:
https://developer.android.com/develop/adaptive-apps/guides/get-started-with-adaptive-apps

## Checklist de aceite

- [ ] Inspecionar cada tela existente e evitar alterações visuais arbitrárias.
- [ ] Verificar o uso de dp/sp e os insets.
- [ ] Testar teclado e fonte ampliada.
- [ ] Testar pelo menos API 26, uma API intermediária e a mais recente suportada pelo toolchain.
- [ ] Testar emuladores de largura compacta, média e expandida.
- [ ] Instalar e validar no POCO X5 Pro.
- [ ] Comparar screenshots com o Figma antes de declarar fidelidade visual.
- [ ] Registrar commit, workflow e resultados de cada teste.
