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
    private const string CacheKey = "fitness_news_v4";

    private static readonly string[] FitnessKeywords =
    [
        "тренировк", "фитнес", "упражнени", "мышц", "бег", "бега", "бегать",
        "питани", "калори", "белок", "протеин", "похудени", "вес", "жир",
        "спортзал", "зал", "кардио", "силовой", "растяжк", "йога", "плавани",
        "здоровь", "иммунитет", "витамин", "сон", "восстановлени", "выносливост",
        "диет", "рацион", "спортивн", "атлет", "марафон", "велосипед", "ходьб"
    ];

    private static readonly (string Url, string Name)[] RssFeeds =
    [
        ("https://lenta.ru/rss/news/wellness/", "Lenta.ru"),
        ("https://lenta.ru/rss/news/sport/", "Lenta.ru"),
    ];

    [HttpGet]
    public async Task<IActionResult> GetNews()
    {
        if (cache.TryGetValue(CacheKey, out List<NewsItem>? cached))
            return Ok(cached);

        var client = httpFactory.CreateClient();
        client.Timeout = TimeSpan.FromSeconds(10);
        client.DefaultRequestHeaders.UserAgent.ParseAdd("Mozilla/5.0 (compatible; Bot/1.0)");

        var all = new List<NewsItem>();

        // 1. InstructorPro — WordPress REST API
        try
        {
            var json = await client.GetStringAsync(
                "https://instructorpro.ru/wp-json/wp/v2/posts?per_page=15&_fields=title,link,excerpt,date&_embed=1");
            using var doc = JsonDocument.Parse(json);
            foreach (var post in doc.RootElement.EnumerateArray())
            {
                var title = System.Net.WebUtility.HtmlDecode(
                    post.GetProperty("title").GetProperty("rendered").GetString() ?? "");
                var url   = post.GetProperty("link").GetString() ?? "";
                var date  = post.GetProperty("date").GetString() ?? DateTime.UtcNow.ToString("o");
                var desc  = System.Net.WebUtility.HtmlDecode(
                    System.Text.RegularExpressions.Regex.Replace(
                        post.GetProperty("excerpt").GetProperty("rendered").GetString() ?? "", "<[^>]+>", "")).Trim();
                if (desc.Length > 200) desc = desc[..200] + "…";

                string? img = null;
                if (post.TryGetProperty("_embedded", out var emb) &&
                    emb.TryGetProperty("wp:featuredmedia", out var media) &&
                    media.ValueKind == JsonValueKind.Array &&
                    media.GetArrayLength() > 0)
                {
                    img = media[0].TryGetProperty("source_url", out var src) ? src.GetString() : null;
                    // encode cyrillic filename if needed
                    if (img != null)
                    {
                        var lastSlash = img.LastIndexOf('/');
                        if (lastSlash >= 0)
                        {
                            var fileName = Uri.EscapeDataString(img[(lastSlash + 1)..]);
                            img = img[..(lastSlash + 1)] + fileName;
                        }
                    }
                }

                if (!string.IsNullOrWhiteSpace(title) && !string.IsNullOrWhiteSpace(url))
                    all.Add(new NewsItem(title, desc, img, url, "InstructorPro", date));
            }
        }
        catch { /* skip */ }

        // 2. Lenta.ru RSS feeds (fitness-filtered)
        foreach (var (feedUrl, sourceName) in RssFeeds)
        {
            try
            {
                var xml = await client.GetStringAsync(feedUrl);
                var doc = XDocument.Parse(xml);
                XNamespace media = "http://search.yahoo.com/mrss/";

                var items = doc.Descendants("item")
                    .Take(50)
                    .Select(item =>
                    {
                        var title = item.Element("title")?.Value?.Trim() ?? "";
                        var link  = item.Element("link")?.Value?.Trim()
                                 ?? item.Elements().FirstOrDefault(e => e.Name.LocalName == "link")?.Value?.Trim()
                                 ?? "";
                        var desc  = item.Element("description")?.Value;
                        var pub   = item.Element("pubDate")?.Value ?? DateTime.UtcNow.ToString("o");

                        if (desc != null)
                            desc = System.Text.RegularExpressions.Regex.Replace(desc, "<[^>]+>", "").Trim();
                        if (desc?.Length > 200) desc = desc[..200] + "…";

                        var img = item.Element(media + "content")?.Attribute("url")?.Value
                               ?? item.Element(media + "thumbnail")?.Attribute("url")?.Value
                               ?? item.Element("enclosure")?.Attribute("url")?.Value;

                        return new NewsItem(title, desc, img, link, sourceName, pub);
                    })
                    .Where(n => !string.IsNullOrWhiteSpace(n.Title) && !string.IsNullOrWhiteSpace(n.Url)
                                && FitnessKeywords.Any(kw =>
                                    n.Title.Contains(kw, StringComparison.OrdinalIgnoreCase) ||
                                    (n.Description?.Contains(kw, StringComparison.OrdinalIgnoreCase) ?? false)))
                    .Take(6)
                    .ToList();

                all.AddRange(items);
            }
            catch { /* skip */ }
        }

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
            "https://www.sports.ru/fitness/", "Sports.ru Фитнес", DateTime.UtcNow.ToString("o")),
        new("Как правильно питаться при похудении", "Советы нутрициологов по составлению рациона для снижения веса",
            "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=400&q=80",
            "https://lenta.ru/wellness/", "Lenta.ru", DateTime.UtcNow.AddHours(-2).ToString("o")),
        new("5 причин начать бегать по утрам", "Утренние пробежки улучшают метаболизм и повышают тонус",
            "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=400&q=80",
            "https://lenta.ru/sport/", "Lenta.ru", DateTime.UtcNow.AddHours(-4).ToString("o")),
        new("Протеин: всё что нужно знать", "Нормы потребления белка для спортсменов и обычных людей",
            "https://images.unsplash.com/photo-1593095948071-474c5cc2989d?w=400&q=80",
            "https://lenta.ru/wellness/", "Lenta.ru", DateTime.UtcNow.AddHours(-6).ToString("o")),
        new("Восстановление после тренировки", "Почему отдых так же важен, как и сама тренировка",
            "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400&q=80",
            "https://lenta.ru/wellness/", "Lenta.ru", DateTime.UtcNow.AddHours(-8).ToString("o")),
        new("Плавание: польза для всего тела", "Как водные тренировки развивают выносливость и сжигают калории",
            "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=400&q=80",
            "https://lenta.ru/sport/", "Lenta.ru", DateTime.UtcNow.AddHours(-10).ToString("o")),
    ];
}
