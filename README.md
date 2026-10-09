# Sabor & Afeto

Aplicativo Android de catálogo de receitas, criado para praticar navegação com Jetpack Compose.

## Tema e telas

O tema escolhido é um catálogo culinário chamado **Sabor & Afeto**. O app inclui:

- Splash que encaminha automaticamente para Login ou Início conforme a sessão salva.
- Login demonstrativo e painel principal.
- Navegação inferior com Início, Receitas e Favoritos.
- Lista rolável de receitas, mensagem de estado vazio e detalhe aberto por identificador tipado.
- Botão para salvar e remover receitas dos Favoritos.

As rotas são `@Serializable` e implementam `AppRoute`. A tela de detalhe recebe um `recipeId` em `RecipeDetail` e recupera a rota com `toRoute()`. O mesmo `RecipeViewModel`, criado no host que contém o `NavHost`, é compartilhado com as telas.

## Como executar

1. Abra a pasta do projeto no Android Studio.
2. Aguarde a sincronização do Gradle.
3. Selecione um emulador ou dispositivo Android (API 24 ou superior).
4. Execute a configuração `app`.

O acesso é demonstrativo: os campos de e-mail e senha não fazem autenticação remota. O botão **Entrar** ou **Continuar como visitante** grava a sessão local para que a Splash abra o painel nas próximas execuções.
