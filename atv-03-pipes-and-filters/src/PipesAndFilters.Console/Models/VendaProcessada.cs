using CsvHelper.Configuration.Attributes;

namespace PipesAndFilters.Console.Models
{
    public sealed record VendaProcessada : Venda
    {
        public VendaProcessada(Venda venda, decimal valorTotal)
        {
            IdVenda = venda.IdVenda;
            Produto = venda.Produto;
            Quantidade = venda.Quantidade;
            PrecoUnitario = venda.PrecoUnitario;
            ValorTotal = valorTotal;
        }

        [Name("valor_total")]
        public decimal ValorTotal { get; private set; }
    }
}
