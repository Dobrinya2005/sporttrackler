using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Caching.Memory;
using System.Text.Json;

namespace FitnessTrainerAPI.Controllers;

public record NewsItem(
    string Title,
    string? Description,
    string? ImageUrl,
    string Url,
    string Source,
    string PublishedAt
);

[ApiController]
[Route("api/news")]
[Authorize]
public class NewsController(IHttpClientFactory httpFactory, IMemoryCache cache) : ControllerBase
{
    private const string CacheKey = "sports_news_v2";

    [HttpGet]
    public async Task<IActionResult> GetNews()
    {
        if (cache.TryGetValue(CacheKey, out List<NewsItem>? cached))
            return Ok(cached);

        var items = await FetchGuardian() ?? FallbackNews();
        cache.Set(CacheKey, items, TimeSpan.FromMinutes(30));
        return Ok(items);
    }

    private async Task<List<NewsItem>?> FetchGuardian()
    {
        try
        {
            var client = httpFactory.CreateClient();
            client.Timeout = TimeSpan.FromSeconds(10);

            var url = "https://content.guardianapis.com/search" +
                      "?section=sport&show-fields=thumbnail,trailText,headline" +
                      "&page-size=20&api-key=test";

            var json = await client.GetStringAsync(url);
            using var doc = JsonDocument.Parse(json);

            var results = doc.RootElement
                .GetProperty("response")
                .GetProperty("results");

            var list = results.EnumerateArray()
                .Select(r =>
                {
                    var fields = r.TryGetProperty("fields", out var f) ? f : (JsonElement?)null;
                    var title  = fields?.TryGetProperty("headline", out var h) == true
                                 ? h.GetString() ?? r.GetProperty("webTitle").GetString() ?? ""
                                 : r.GetProperty("webTitle").GetString() ?? "";
                    var desc   = fields?.TryGetProperty("trailText", out var t) == true ? t.GetString() : null;
                    var img    = fields?.TryGetProperty("thumbnail", out var th) == true ? th.GetString() : null;
                    var url2   = r.GetProperty("webUrl").GetString() ?? "";
                    var pub    = r.GetProperty("webPublicationDate").GetString() ?? "";

                    return new NewsItem(title, desc, img, url2, "The Guardian", pub);
                })
                .Where(n => !string.IsNullOrWhiteSpace(n.Title))
                .ToList();

            return list.Count > 0 ? list : null;
        }
        catch
        {
            return null;
        }
    }

    private static List<NewsItem> FallbackNews() =>
    [
        new("Топ-10 упражнений для роста мышц", "Лучшие упражнения для набора мышечной массы по мнению тренеров", null, "https://www.championat.com", "Чемпионат", DateTime.UtcNow.ToString("o")),
        new("Как правильно питаться при похудении", "Советы нутрициологов по составлению рациона для снижения веса", null, "https://sportrbc.ru", "РБК Спорт", DateTime.UtcNow.AddHours(-2).ToString("o")),
        new("5 причин начать бегать по утрам", "Утренние пробежки улучшают метаболизм и повышают тонус", null, "https://rsport.ria.ru", "РИА Спорт", DateTime.UtcNow.AddHours(-4).ToString("o")),
        new("Протеин: всё что нужно знать", "Нормы потребления белка для спортсменов и обычных людей", null, "https://www.championat.com", "Чемпионат", DateTime.UtcNow.AddHours(-6).ToString("o")),
        new("Восстановление после тренировки", "Почему отдых так же важен, как и сама тренировка", null, "https://sportrbc.ru", "РБК Спорт", DateTime.UtcNow.AddHours(-8).ToString("o")),
    ];
}
