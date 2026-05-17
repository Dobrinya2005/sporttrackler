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
                    <div style="font-family:Arial,sans-serif;max-width:420px;margin:0 auto;background:#1e1e2e;color:#cdd6f4;border-radius:16px;overflow:hidden;">
                      <!-- header strip -->
                      <div style="background:linear-gradient(135deg,#1a1f45 0%,#252545 100%);padding:20px 28px 16px;border-bottom:1px solid #313244;">
                        <table width="100%" cellpadding="0" cellspacing="0"><tr>
                          <td>
                            <span style="font-size:20px;font-weight:bold;color:#89b4fa;letter-spacing:0.5px;">SportTrackler</span>
                          </td>
                          <td align="right">
                            <!-- dumbbell icon (SVG inline) -->
                            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style="opacity:0.55;vertical-align:middle;">
                              <rect x="1.5" y="9" width="3" height="6" rx="1.5" fill="#89b4fa"/>
                              <rect x="4.5" y="7" width="2.5" height="10" rx="1.25" fill="#89b4fa"/>
                              <rect x="7" y="11" width="10" height="2" rx="1" fill="#89b4fa"/>
                              <rect x="17" y="7" width="2.5" height="10" rx="1.25" fill="#89b4fa"/>
                              <rect x="19.5" y="9" width="3" height="6" rx="1.5" fill="#89b4fa"/>
                            </svg>
                          </td>
                        </tr></table>
                      </div>
                      <!-- body -->
                      <div style="position:relative;padding:28px 28px 24px;overflow:hidden;">
                        <!-- watermark dumbbell -->
                        <div style="position:absolute;right:-10px;bottom:-10px;opacity:0.06;pointer-events:none;">
                          <svg width="160" height="160" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <rect x="1.5" y="9" width="3" height="6" rx="1.5" fill="#cdd6f4"/>
                            <rect x="4.5" y="7" width="2.5" height="10" rx="1.25" fill="#cdd6f4"/>
                            <rect x="7" y="11" width="10" height="2" rx="1" fill="#cdd6f4"/>
                            <rect x="17" y="7" width="2.5" height="10" rx="1.25" fill="#cdd6f4"/>
                            <rect x="19.5" y="9" width="3" height="6" rx="1.5" fill="#cdd6f4"/>
                          </svg>
                        </div>
                        <p style="margin:0 0 16px;color:#cdd6f4;font-size:15px;">{mailHeading}</p>
                        <div style="font-size:42px;font-weight:bold;letter-spacing:14px;color:#a6e3a1;padding:12px 0;font-family:monospace;">{code}</div>
                        <p style="margin:20px 0 0;color:#6c7086;font-size:13px;">Код действителен 10 минут.</p>
                      </div>
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
