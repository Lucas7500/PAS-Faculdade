namespace PipesAndFilters.Console.Abstractions
{
    public interface IFilter<in TInput, out TOutput>
    {
        TOutput Process(TInput input);
    }
}
