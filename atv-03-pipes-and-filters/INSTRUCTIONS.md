## Exercício prático — Pipes and Filters

## Contexto

Uma empresa possui um arquivo CSV contendo registros de vendas. O arquivo precisa ser processado para gerar um relatório consolidado.

O processamento deverá ser organizado utilizando o padrão arquitetural Pipes and Filters.

Cada etapa do processamento deverá ser implementada como um filtro independente, com responsabilidade bem definida e uma entrada e uma saída claramente especificadas.

Arquivo de entrada

O arquivo possui as seguintes colunas:

```
id_venda, produto, quantidade, preco_unitario
```

Exemplo:

```
001,Notebook,2,4500.00
002,Mouse,5,90.00
003,Monitor,1,1200.00
```

Obs: Em anexo teremos dois arquivos exemplos na atividade, o arquivo com 10 registros deve ser utilizado inicialmente para desenvolvimento e testes. O arquivo com 1000 registros deve ser utilizado posteriormente para verificar se a solução continua funcionando adequadamente com um volume maior de dados.

## Requisitos do pipeline

A solução deverá possuir, no mínimo, os seguintes filtros:

## 1. Filtro de leitura

Responsável por:

- ler o arquivo CSV;

- transformar cada registro em uma estrutura adequada para processamento;

- encaminhar os registros para o próximo filtro.

```
CSV
↓
[ReadFilter]
```


```
2. Filtro de limpeza e validação
Responsável por identificar registros inválidos.
Considere inválido um registro que possua, por exemplo:
● quantidade menor ou igual a zero;
● preço unitário ausente;
● dados numéricos inválidos.
Os registros inválidos devem ser descartados ou tratados de maneira claramente documentada.
[ReadFilter]
↓
[CleanFilter]
3. Filtro de transformação
Para cada venda válida, calcular:
valor_total = quantidade × preco_unitario
Por exemplo:
Produto: Mouse
Quantidade: 5
Preço unitário: 90,00
Valor total: 450,00
[CleanFilter]
↓
[TransformFilter]
4. Filtro de agregação
Calcular pelo menos:
● quantidade total de produtos vendidos;
● valor total das vendas;
● quantidade de vendas válidas.
[TransformFilter]
↓
[SumFilter]
```


## 5. Filtro de geração do relatório

Produzir uma saída contendo os resultados consolidados.

## Exemplo:

========================================

RELATÓRIO DE VENDAS

========================================

Vendas válidas:

Produtos vendidos:

Valor total: R\$ 14.630,00

========================================

8

20

## Visão geral do Pipeline completo

[Arquivo]

│

[ReadFilter]

│

[CleanFilter]

│

[TransformFilter]

│

[SumFilter]

│

[ReportFilter]

│

[Relatório]


## Resultado esperado para o arquivo de 10 registros

O arquivo de 10 registros contém 8 registros válidos. Dois registros devem ser identificados como inválidos durante a etapa de limpeza/validação.

## A saída esperada é:

========================================

RELATÓRIO DE VENDAS

========================================

Vendas válidas:

Produtos vendidos: 20

Valor total: R\$ 14.630,00

========================================

8

## Requisito arquitetural

A atividade não consiste apenas em produzir o resultado correto. A implementação deverá demonstrar efetivamente o uso do padrão Pipes and Filters.

## Cada filtro deverá:

- possuir uma responsabilidade específica;

- ser independente dos demais filtros quanto à sua implementação;

- receber uma entrada e produzir uma saída;

- não conhecer detalhes internos dos outros filtros;

- poder ser testado isoladamente.
