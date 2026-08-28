using PipesAndFilters.Console.Abstractions;
using PipesAndFilters.Console.Models;

namespace PipesAndFilters.Console.Filters
{
    public sealed class SumFilter : IFilter<IReadOnlyCollection<VendaProcessada>, RelatorioVendas>
    {
        public RelatorioVendas Process(IReadOnlyCollection<VendaProcessada> input)
        {
            return new RelatorioVendas
            {
                QuantidadeVendasValidas = input.Count,
                QuantidadeTotalProdutos = input.Sum(venda => venda.Quantidade ?? 0),
                ValorTotalVendas = input.Sum(venda => venda.ValorTotal)
            };
        }
    }
}
