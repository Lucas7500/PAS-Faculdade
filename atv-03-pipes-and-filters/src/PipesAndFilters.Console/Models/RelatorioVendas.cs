namespace PipesAndFilters.Console.Models
{
    public sealed record RelatorioVendas
    {
        public int QuantidadeVendasValidas { get; set; }
        public int QuantidadeTotalProdutos { get; set; }
        public decimal ValorTotalVendas { get; set; }
    }
}
