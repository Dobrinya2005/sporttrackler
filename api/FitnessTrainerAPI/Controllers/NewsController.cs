using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Caching.Memory;
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
    private const string CacheKey = "sports_news";

    private static readonly (string Url, string Name)[] Feeds =
    [
        ("https://rsport.ria.ru/export/rss2/sport/index.xml", "РИА Спорт"),
        ("https://www.championat.com/rss/", "Чемпионат"),
        ("https://sportrbc.ru/rss", "РБК Спорт"),
    ];

    [HttpGet]
    public async Task<IActionResult> GetNews()
    {
        if (cache.TryGetValue(CacheKey, out List<NewsItem>? cached))
            return Ok(cached);

        var client = httpFactory.CreateClient();
        client.DefaultRequestHeaders.UserAgent.ParseAdd(
            "Mozilla/5.0 (compatible; SportTrackler/1.0)");

        var all = new List<NewsItem>();

        foreach (var (feedUrl, sourceName) in Feeds)
        {
            try
            {
                var xml = await client.GetStringAsync(feedUrl);
                var doc = XDocument.Parse(xml);
                XNamespace media = "http://search.yahoo.com/mrss/";

                var items = doc.Descendants("item")
                    .Take(10)
                    .Select(item =>
                    {
                        var title = item.Element("title")?.Value ?? "";
                        var link  = item.Element("link")?.Value
                                 ?? item.Element("{http://www.w3.org/2005/Atom}link")?.Attribute("href")?.Value
                                 ?? "";
                        var desc  = item.Element("description")?.Value;
                        var pub   = item.Element("pubDate")?.Value ?? "";

                        var img = item.Element(media + "content")?.Attribute("url")?.Value
                               ?? item.Element(media + "thumbnail")?.Attribute("url")?.Value
                               ?? item.Element("enclosure")?.Attribute("url")?.Value;

                        // strip HTML from description
                        if (desc != null)
                            desc = System.Text.RegularExpressions.Regex.Replace(desc, "<[^>]+>", "").Trim();

                        return new NewsItem(title.Trim(), desc, img, link, sourceName, pub);
                    })
                    .Where(n => !string.IsNullOrWhiteSpace(n.Title) && !string.IsNullOrWhiteSpace(n.Url))
                    .ToList();

                all.AddRange(items);
            }
            catch { /* skip failed feed */ }
        }

        // interleave sources so feed looks varied
        all = all.OrderBy(_ => Guid.NewGuid()).ToList();

        cache.Set(CacheKey, all, TimeSpan.FromMinutes(30));
        return Ok(all);
    }
}
