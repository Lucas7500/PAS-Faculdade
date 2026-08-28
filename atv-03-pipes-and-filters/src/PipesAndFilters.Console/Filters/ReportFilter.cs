using System.Globalization;
using System.Text;
using PipesAndFilters.Console.Abstractions;
using PipesAndFilters.Console.Models;

namespace PipesAndFilters.Console.Filters
{
    public sealed class ReportFilter : IFilter<RelatorioVendas, string>
    {
        public string Process(RelatorioVendas input)
        {
            CultureInfo culture = new("pt-BR");
            StringBuilder sb = new();

            sb.AppendLine("========================================");
            sb.AppendLine("RELATÓRIO DE VENDAS");
            sb.AppendLine("========================================");
            sb.AppendLine($"Vendas válidas: {input.QuantidadeVendasValidas}");
            sb.AppendLine($"Produtos vendidos: {input.QuantidadeTotalProdutos}");
            sb.AppendLine($"Valor total: {input.ValorTotalVendas.ToString("C", culture)}");
            sb.AppendLine("========================================");

            return sb.ToString();
        }
    }
}
