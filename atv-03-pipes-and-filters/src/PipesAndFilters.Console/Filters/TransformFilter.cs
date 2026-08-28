using PipesAndFilters.Console.Abstractions;
using PipesAndFilters.Console.Models;

namespace PipesAndFilters.Console.Filters
{
    public sealed class TransformFilter : IFilter<IReadOnlyCollection<Venda>, IReadOnlyCollection<VendaProcessada>>
    {
        public IReadOnlyCollection<VendaProcessada> Process(IReadOnlyCollection<Venda> input)
        {
            return input
                .Select(venda =>
                {
                    decimal valorTotal = (venda.Quantidade ?? 0) * (venda.PrecoUnitario ?? 0);
                    return new VendaProcessada(venda, valorTotal);
                })
                .ToList()
                .AsReadOnly();
        }
    }
}
