# Leitor Fiscal
Um aplicativo Android desenvolvido em Kotlin para digitalizar cupons fiscais, extrair informações de produtos e detalhes da compra usando OCR (Reconhecimento Óptico de Caracteres) e persistir os dados localmente em um banco de dados SQLite.

O projeto foi desenvolvido com foco inicial em um formato de cupom específico (Lojas Renner) para validar o fluxo completo de digitalização, extração, salvamento e gerenciamento de dados, com menção especial ao contexto de cupons fiscais de Fortaleza, Ceará.

# Funcionalidades Implementadas

* Digitalização e OCR: Seleção de imagens da galeria e extração de texto via Google ML Kit Text Recognition.
* Parsing Inteligente: Lógica de parsing customizada para extrair dados estruturados (informações do estabelecimento e lista de produtos) a partir do texto OCR bruto e fragmentado.
* Persistência de Dados:
- Salva os cupons e produtos extraídos em um banco de dados SQLite local.
- A implementação utiliza SQLiteOpenHelper nativo para criação e gerenciamento do banco.
* Gerenciamento de Cupons Salvos:
- Listagem: Tela dedicada que exibe todos os cupons salvos, ordenados pelo mais recente.
- Deleção: Funcionalidade para deletar permanentemente um cupom salvo, protegida por um diálogo de confirmação.
* Navegação Moderna: Uso do Jetpack Navigation Component para gerenciar a navegação entre as telas do aplicativo, incluindo um menu lateral (Navigation Drawer).
* Internacionalização (i18n): Suporte para Português, Inglês e Espanhol.

# Tecnologias Utilizadas

* Linguagem: Kotlin (versão 2.x)
* Arquitetura: MVVM (Model-View-ViewModel)
* UI: Android Views com View Binding
* Assincronismo: Kotlin Coroutines para operações de I/O (leitura/escrita no banco de dados) e outras tarefas em background.
* Banco de Dados: SQLite, implementado com SQLiteOpenHelper e abstraído por uma classe DataSource.
* Build: Gradle com Version Catalogs (libs.versions.toml) e KSP (Kotlin Symbol Processing).