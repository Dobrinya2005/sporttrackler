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
        new("Топ-10 упражнений для роста мышц", "Лучшие упражнения для набора мышечной массы по мнению тренеров",
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=400&q=80",
            "https://www.championat.com/football/", "Чемпионат", DateTime.UtcNow.ToString("o")),
        new("Как правильно питаться при похудении", "Советы нутрициологов по составлению рациона для снижения веса",
            "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=400&q=80",
            "https://sportrbc.ru", "РБК Спорт", DateTime.UtcNow.AddHours(-2).ToString("o")),
        new("5 причин начать бегать по утрам", "Утренние пробежки улучшают метаболизм и повышают тонус",
            "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=400&q=80",
            "https://rsport.ria.ru", "РИА Спорт", DateTime.UtcNow.AddHours(-4).ToString("o")),
        new("Протеин: всё что нужно знать", "Нормы потребления белка для спортсменов и обычных людей",
            "https://images.unsplash.com/photo-1593095948071-474c5cc2989d?w=400&q=80",
            "https://www.championat.com", "Чемпионат", DateTime.UtcNow.AddHours(-6).ToString("o")),
        new("Восстановление после тренировки", "Почему отдых так же важен, как и сама тренировка",
            "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400&q=80",
            "https://sportrbc.ru", "РБК Спорт", DateTime.UtcNow.AddHours(-8).ToString("o")),
        new("Плавание: польза для всего тела", "Как водные тренировки развивают выносливость и сжигают калории",
            "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=400&q=80",
            "https://rsport.ria.ru", "РИА Спорт", DateTime.UtcNow.AddHours(-10).ToString("o")),
    ];
}
