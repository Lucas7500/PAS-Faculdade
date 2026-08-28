using PipesAndFilters.Console.Abstractions;
using PipesAndFilters.Console.Models;

namespace PipesAndFilters.Console.Filters
{
    public sealed class CleanFilter : IFilter<IReadOnlyCollection<Venda>, IReadOnlyCollection<Venda>>
    {
        public IReadOnlyCollection<Venda> Process(IReadOnlyCollection<Venda> input)
        {
            List<Venda> validVendas = [];

            foreach (Venda venda in input)
            {
                if (IsValid(venda))
                {
                    validVendas.Add(venda);
                }
                else
                {
                    System.Console.WriteLine($"[CleanFilter] Aviso: Registro ignorado por ser inválido. IdVenda: '{venda.IdVenda}', Produto: '{venda.Produto}', Quantidade: {venda.Quantidade}, Preço: {venda.PrecoUnitario}");
                }
            }

            return validVendas.AsReadOnly();
        }

        private static bool IsValid(Venda venda)
        {
            if (!venda.Quantidade.HasValue || venda.Quantidade.Value <= 0)
                return false;

            if (!venda.PrecoUnitario.HasValue || venda.PrecoUnitario.Value <= 0)
                return false;

            if (string.IsNullOrWhiteSpace(venda.IdVenda) || string.IsNullOrWhiteSpace(venda.Produto))
                return false;

            return true;
        }
    }
}
