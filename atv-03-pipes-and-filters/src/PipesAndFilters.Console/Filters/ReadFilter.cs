using CsvHelper;
using CsvHelper.Configuration;
using CsvHelper.TypeConversion;
using PipesAndFilters.Console.Abstractions;
using PipesAndFilters.Console.Models;
using System.Globalization;

namespace PipesAndFilters.Console.Filters
{
    public sealed class ReadFilter : IFilter<string, IReadOnlyCollection<Venda>>
    {
        public IReadOnlyCollection<Venda> Process(string input)
        {
            CsvConfiguration config = new(CultureInfo.InvariantCulture);

            using StreamReader reader = new(input);
            using CsvReader csv = new(reader, config);
            
            csv.Context.TypeConverterCache.AddConverter<int?>(new SafeNullableIntConverter());
            csv.Context.TypeConverterCache.AddConverter<decimal?>(new SafeNullableDecimalConverter());

            return csv.GetRecords<Venda>().ToList().AsReadOnly();
        }

        private sealed class SafeNullableIntConverter : DefaultTypeConverter
        {
            public override object? ConvertFromString(string? text, IReaderRow row, MemberMapData memberMapData)
            {
                if (string.IsNullOrWhiteSpace(text)) 
                    return null;

                if (int.TryParse(text, NumberStyles.Any, CultureInfo.InvariantCulture, out var result)) 
                    return result;
                
                return null;
            }
        }

        private sealed class SafeNullableDecimalConverter : DefaultTypeConverter
        {
            public override object? ConvertFromString(string? text, IReaderRow row, MemberMapData memberMapData)
            {
                if (string.IsNullOrWhiteSpace(text)) 
                    return null;
                
                if (decimal.TryParse(text, NumberStyles.Any, CultureInfo.InvariantCulture, out var result)) 
                    return result;
                
                return null;
            }
        }
    }
}
