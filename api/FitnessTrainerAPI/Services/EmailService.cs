using System.Net;
using System.Net.Mail;

namespace FitnessTrainerAPI.Services;

public interface IEmailService
{
    Task SendVerificationCodeAsync(string toEmail, string code);
}

public class EmailService(IConfiguration config, ILogger<EmailService> logger) : IEmailService
{
    public async Task SendVerificationCodeAsync(string toEmail, string code)
    {
        logger.LogInformation("EMAIL VERIFICATION CODE for {Email}: {Code}", toEmail, code);

        var smtpHost  = config["Smtp:Host"];
        var smtpPort  = int.TryParse(config["Smtp:Port"], out var p) ? p : 587;
        var smtpUser  = config["Smtp:Username"];
        var smtpPass  = config["Smtp:Password"];
        var fromEmail = config["Smtp:FromEmail"] ?? smtpUser;
        var fromName  = config["Smtp:FromName"] ?? "SportTrackler";

        if (string.IsNullOrEmpty(smtpHost) || string.IsNullOrEmpty(smtpUser) || string.IsNullOrEmpty(smtpPass))
        {
            logger.LogWarning("SMTP не настроен — письмо не отправлено. Код: {Code}", code);
            return;
        }

        try
        {
            using var client = new SmtpClient(smtpHost, smtpPort)
            {
                Credentials = new NetworkCredential(smtpUser, smtpPass),
                EnableSsl   = true,
                Timeout     = 5000
            };

            var mail = new MailMessage
            {
                From       = new MailAddress(fromEmail!, fromName),
                Subject    = "Код подтверждения SportTrackler",
                IsBodyHtml = true,
                Body       = $"""
                    <div style="font-family:Arial,sans-serif;max-width:400px;margin:0 auto;padding:24px;background:#1e1e2e;color:#cdd6f4;border-radius:12px;">
                      <h2 style="color:#89b4fa;margin-bottom:8px;">SportTrackler</h2>
                      <p>Ваш код подтверждения:</p>
                      <div style="font-size:36px;font-weight:bold;letter-spacing:12px;color:#a6e3a1;padding:16px 0;">{code}</div>
                      <p style="color:#6c7086;font-size:13px;">Код действителен 10 минут.</p>
                    </div>
                    """
            };
            mail.To.Add(toEmail);

            await client.SendMailAsync(mail);
            logger.LogInformation("EMAIL SENT OK to {Email}", toEmail);
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "Failed to send email to {Email}: {Msg}", toEmail, ex.Message);
        }
    }
}
