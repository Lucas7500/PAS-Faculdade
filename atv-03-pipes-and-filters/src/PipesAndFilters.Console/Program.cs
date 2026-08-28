using PipesAndFilters.Console.Filters;
using PipesAndFilters.Console.Models;

Console.WriteLine("Iniciando o Pipeline de Vendas...\n");

//string inputFilePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "..", "..", "..", "..", "..", "input", "vendas_exemplo_10_linhas.csv");

// Para testar o outro arquivo:
 string inputFilePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "..", "..", "..", "..", "..", "input", "vendas_exemplo_1000_linhas_com_invalidas.csv");

ReadFilter readFilter = new();
CleanFilter cleanFilter = new();
TransformFilter transformFilter = new();
SumFilter sumFilter = new();
ReportFilter reportFilter = new();

try
{
    IReadOnlyCollection<Venda> records = readFilter.Process(inputFilePath);
    IReadOnlyCollection<Venda> cleanRecords = cleanFilter.Process(records);
    IReadOnlyCollection<VendaProcessada> transformedRecords = transformFilter.Process(cleanRecords);
    RelatorioVendas summary = sumFilter.Process(transformedRecords);
    string reportOutput = reportFilter.Process(summary);

    Console.WriteLine(reportOutput);
}
catch (Exception ex)
{
    Console.WriteLine($"Erro no processamento: {ex.Message}");
}
