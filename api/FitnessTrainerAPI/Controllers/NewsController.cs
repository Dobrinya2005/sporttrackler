using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Caching.Memory;
using System.Text.Json;
using System.Xml.Linq;

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
    private const string CacheKey = "sports_news_v3";

    private static readonly (string Url, string Name)[] RssFeeds =
    [
        ("https://www.championat.com/rss/", "Чемпионат"),
        ("https://rsport.ria.ru/export/rss2/sport/index.xml", "РИА Спорт"),
        ("https://sportrbc.ru/rss", "РБК Спорт"),
    ];

    [HttpGet]
    public async Task<IActionResult> GetNews()
    {
        if (cache.TryGetValue(CacheKey, out List<NewsItem>? cached))
            return Ok(cached);

        var client = httpFactory.CreateClient();
        client.Timeout = TimeSpan.FromSeconds(8);
        client.DefaultRequestHeaders.UserAgent.ParseAdd("Mozilla/5.0 (compatible; Bot/1.0)");

        var all = new List<NewsItem>();

        foreach (var (feedUrl, sourceName) in RssFeeds)
        {
            try
            {
                var xml = await client.GetStringAsync(feedUrl);
                var doc = XDocument.Parse(xml);
                XNamespace media = "http://search.yahoo.com/mrss/";

                var items = doc.Descendants("item")
                    .Take(8)
                    .Select(item =>
                    {
                        var title = item.Element("title")?.Value?.Trim() ?? "";
                        var link  = item.Element("link")?.Value?.Trim()
                                 ?? item.Elements().FirstOrDefault(e => e.Name.LocalName == "link")?.Value?.Trim()
                                 ?? "";
                        var desc  = item.Element("description")?.Value;
                        var pub   = item.Element("pubDate")?.Value ?? DateTime.UtcNow.ToString("o");

                        // strip CDATA / HTML
                        if (desc != null)
                            desc = System.Text.RegularExpressions.Regex
                                .Replace(desc, "<[^>]+>", "").Trim();
                        if (desc?.Length > 200) desc = desc[..200] + "…";

                        // try various image locations
                        var img = item.Element(media + "content")?.Attribute("url")?.Value
                               ?? item.Element(media + "thumbnail")?.Attribute("url")?.Value
                               ?? item.Element("enclosure")?.Attribute("url")?.Value;

                        return new NewsItem(title, desc, img, link, sourceName, pub);
                    })
                    .Where(n => !string.IsNullOrWhiteSpace(n.Title) && !string.IsNullOrWhiteSpace(n.Url))
                    .ToList();

                all.AddRange(items);
            }
            catch { /* skip failed feed */ }
        }

        // fallback if all RSS feeds failed
        if (all.Count == 0)
            all = FallbackNews();

        all = [.. all.OrderBy(_ => Guid.NewGuid())];
        cache.Set(CacheKey, all, TimeSpan.FromMinutes(30));
        return Ok(all);
    }

    private static List<NewsItem> FallbackNews() =>
    [
        new("Топ-10 упражнений для роста мышц", "Лучшие упражнения для набора мышечной массы", null, "https://www.championat.com/football/", "Чемпионат", DateTime.UtcNow.ToString("o")),
        new("Как правильно питаться при похудении", "Советы нутрициологов по составлению рациона", null, "https://sportrbc.ru", "РБК Спорт", DateTime.UtcNow.AddHours(-2).ToString("o")),
        new("5 причин начать бегать по утрам", "Утренние пробежки улучшают метаболизм", null, "https://rsport.ria.ru", "РИА Спорт", DateTime.UtcNow.AddHours(-4).ToString("o")),
        new("Протеин: всё что нужно знать", "Нормы потребления белка для спортсменов", null, "https://www.championat.com", "Чемпионат", DateTime.UtcNow.AddHours(-6).ToString("o")),
        new("Восстановление после тренировки", "Почему отдых так же важен, как и сама тренировка", null, "https://sportrbc.ru", "РБК Спорт", DateTime.UtcNow.AddHours(-8).ToString("o")),
    ];
}
