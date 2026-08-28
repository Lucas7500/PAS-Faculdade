using CsvHelper.Configuration.Attributes;

namespace PipesAndFilters.Console.Models
{
    public record Venda
    {
        [Name("id_venda")]
        public string IdVenda { get; init; } = string.Empty;

        [Name("produto")]
        public string Produto { get; init; } = string.Empty;

        [Name("quantidade")]
        public int? Quantidade { get; init; }

        [Name("preco_unitario")]
        public decimal? PrecoUnitario { get; init; }
    }
}
