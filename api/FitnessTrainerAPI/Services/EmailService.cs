using System.Net;
using System.Net.Mail;

namespace FitnessTrainerAPI.Services;

public interface IEmailService
{
    Task SendVerificationCodeAsync(string toEmail, string code, string? subject = null, string? heading = null);
}

public class EmailService(IConfiguration config, ILogger<EmailService> logger) : IEmailService
{
    public async Task SendVerificationCodeAsync(string toEmail, string code, string? subject = null, string? heading = null)
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

            var mailSubject = subject ?? "Код подтверждения SportTrackler";
            var mailHeading = heading ?? "Ваш код подтверждения:";
            var mail = new MailMessage
            {
                From       = new MailAddress(fromEmail!, fromName),
                Subject    = mailSubject,
                IsBodyHtml = true,
                Body       = $"""
                    <table width="100%" cellpadding="0" cellspacing="0" style="font-family:Arial,sans-serif;background:#f4f4f8;">
                      <tr><td align="center" style="padding:32px 16px;">
                        <table width="420" cellpadding="0" cellspacing="0" style="background:#1e1e2e;border-radius:16px;overflow:hidden;max-width:420px;">
                          <!-- header -->
                          <tr>
                            <td style="background:#1a1f45;padding:18px 24px;border-bottom:1px solid #313244;">
                              <table width="100%" cellpadding="0" cellspacing="0"><tr>
                                <td style="color:#89b4fa;font-size:20px;font-weight:bold;letter-spacing:0.5px;">
                                  🏋️ SportTrackler
                                </td>
                              </tr></table>
                            </td>
                          </tr>
                          <!-- body -->
                          <tr>
                            <td style="padding:28px 24px 8px;">
                              <p style="margin:0 0 20px;color:#cdd6f4;font-size:15px;">{mailHeading}</p>
                              <!-- code row: code left, watermark emoji right -->
                              <table width="100%" cellpadding="0" cellspacing="0"><tr>
                                <td style="font-size:44px;font-weight:bold;letter-spacing:10px;color:#a6e3a1;font-family:monospace;vertical-align:middle;">
                                  {code}
                                </td>
                                <td align="right" style="font-size:72px;color:#2a2a40;vertical-align:middle;padding-left:8px;">
                                  🏋️
                                </td>
                              </tr></table>
                            </td>
                          </tr>
                          <!-- footer -->
                          <tr>
                            <td style="padding:16px 24px 24px;border-top:1px solid #313244;">
                              <p style="margin:0;color:#6c7086;font-size:12px;">Код действителен 10 минут. Не передавайте его никому.</p>
                            </td>
                          </tr>
                        </table>
                      </td></tr>
                    </table>
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
