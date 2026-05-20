using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Caching.Memory;
using System.Text.Json;
using System.Text.Json.Serialization;

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
    private const string ApiKey   = "601e7e6227254823b35bccdf58dd8f30";
    private const string CacheKey = "sports_news";

    [HttpGet]
    public async Task<IActionResult> GetNews()
    {
        if (cache.TryGetValue(CacheKey, out List<NewsItem>? cached))
            return Ok(cached);

        var client = httpFactory.CreateClient();
        var url = $"https://newsapi.org/v2/top-headlines?category=sports&language=ru&pageSize=20&apiKey={ApiKey}";

        try
        {
            var response = await client.GetAsync(url);
            if (!response.IsSuccessStatusCode)
            {
                // fallback: English sports news
                url = $"https://newsapi.org/v2/top-headlines?category=sports&language=en&pageSize=20&apiKey={ApiKey}";
                response = await client.GetAsync(url);
            }

            var json = await response.Content.ReadAsStringAsync();
            using var doc = JsonDocument.Parse(json);

            var articles = doc.RootElement
                .GetProperty("articles")
                .EnumerateArray()
                .Where(a => a.GetProperty("title").GetString() != "[Removed]")
                .Select(a => new NewsItem(
                    a.GetProperty("title").GetString() ?? "",
                    a.TryGetProperty("description", out var d) ? d.GetString() : null,
                    a.TryGetProperty("urlToImage", out var img) ? img.GetString() : null,
                    a.GetProperty("url").GetString() ?? "",
                    a.GetProperty("source").GetProperty("name").GetString() ?? "",
                    a.GetProperty("publishedAt").GetString() ?? ""
                ))
                .Where(n => !string.IsNullOrWhiteSpace(n.Title))
                .ToList();

            cache.Set(CacheKey, articles, TimeSpan.FromMinutes(30));
            return Ok(articles);
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { message = ex.Message });
        }
    }
}
