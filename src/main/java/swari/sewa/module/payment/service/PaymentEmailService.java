package swari.sewa.module.payment.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import swari.sewa.module.auth.service.EmailService;
import swari.sewa.module.payment.entity.Payment;
import swari.sewa.module.subscription.entity.SubscriptionPlan;
import swari.sewa.module.subscription.service.SubscriptionSettingsService;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.user.repository.ShopOwnerRepository;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEmailService {

    private final EmailService emailService;
    private final ShopOwnerRepository shopOwnerRepository;
    private final SubscriptionSettingsService settingsService;

    @Value("${app.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${app.backend-base-url:http://localhost:8081}")
    private String backendBaseUrl;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    /**
     * Send a payment success email with invoice details and a download link.
     * Called after subscription is activated.
     */
    public void sendPaymentSuccessEmail(Payment payment, SubscriptionPlan plan) {
        try {
            ShopOwner owner = shopOwnerRepository.findById(payment.getShopOwnerId()).orElse(null);
            if (owner == null || owner.getEmail() == null || owner.getEmail().isBlank()) {
                log.warn("Cannot send payment email: shop owner {} has no email", payment.getShopOwnerId());
                return;
            }

            String ownerName = (owner.getFirstName() != null ? owner.getFirstName() : "")
                    + (owner.getLastName() != null ? " " + owner.getLastName() : "").trim();
            if (ownerName.isBlank()) ownerName = owner.getShopName() != null ? owner.getShopName() : "Valued Customer";

            String paidDate = payment.getPaidAt() != null
                    ? payment.getPaidAt().format(DATETIME_FMT) : "—";

            String subStart = "—";
            String subEnd = "—";
            // We don't have the subscription object here, but we can compute from billing cycle
            if (payment.getPaidAt() != null) {
                subStart = payment.getPaidAt().format(DATE_FMT);
                LocalDateTime endDate = calculateEndDate(payment.getPaidAt(), payment.getBillingCycle());
                subEnd = endDate.format(DATE_FMT);
            }

            String invoiceDownloadUrl = frontendBaseUrl + "/shopowner/subscription";

            // Build the email body (without download button — invoice is attached)
            String emailHtml = buildEmailHtml(
                    ownerName,
                    owner.getShopName(),
                    owner.getEmail(),
                    plan.getName(),
                    payment.getBillingCycle(),
                    payment.getInvoiceNumber(),
                    payment.getTransactionUuid(),
                    payment.getGateway(),
                    paidDate,
                    subStart,
                    subEnd,
                    payment.getAmount().toPlainString(),
                    payment.getTaxAmount().toPlainString(),
                    payment.getTotalAmount().toPlainString()
            );

            // Build the invoice PDF HTML (the full invoice template)
            var vatSettings = settingsService.getSettingsEntity();
            String taxLabel = (Boolean.TRUE.equals(vatSettings.getEnableVat()) && vatSettings.getTaxPercentage() != null && vatSettings.getTaxPercentage() > 0)
                    ? "Tax / VAT (" + vatSettings.getTaxPercentage() + "%)"
                    : "Tax / VAT";

            String invoiceHtml = buildInvoicePdfHtml(
                    ownerName, owner.getShopName(), owner.getEmail(), owner.getPhone(),
                    owner.getAddress() != null ? owner.getAddress() : "Nepal",
                    plan.getName(), payment.getBillingCycle(),
                    payment.getInvoiceNumber(), payment.getTransactionUuid(),
                    payment.getGateway(), paidDate, subStart, subEnd,
                    payment.getAmount().toPlainString(),
                    (payment.getDiscountAmount() != null && payment.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0)
                            ? "<tr><td class=\"desc-cell\"><strong>Coupon Discount" +
                              (payment.getCouponCodeSnapshot() != null ? " (" + payment.getCouponCodeSnapshot() + ")" : "") +
                              "</strong></td><td style=\"color:#059669;\">- NPR " + payment.getDiscountAmount().toPlainString() + "</td></tr>"
                            : "",
                    taxLabel,
                    payment.getTaxAmount().toPlainString(),
                    payment.getTotalAmount().toPlainString()
            );

            // Generate PDF from the invoice HTML
            byte[] pdfBytes = generatePdfFromHtml(invoiceHtml);
            String pdfFilename = "Invoice_" + payment.getInvoiceNumber() + ".pdf";

            String subject = "Payment Successful - Invoice " + payment.getInvoiceNumber() + " | Swari Sadhan";
            emailService.sendHtmlEmailWithAttachment(owner.getEmail(), subject, emailHtml,
                    pdfFilename, pdfBytes, "application/pdf");
            log.info("Payment success email with PDF invoice attached sent to {} for invoice {}",
                    owner.getEmail(), payment.getInvoiceNumber());

        } catch (Exception e) {
            log.error("Failed to send payment success email for payment {}: {}",
                    payment.getTransactionUuid(), e.getMessage(), e);
            // Don't throw — email failure should not break payment flow
        }
    }

    private LocalDateTime calculateEndDate(LocalDateTime startDate, String billingCycle) {
        if (billingCycle == null) return startDate.plusMonths(1);
        switch (billingCycle.toLowerCase()) {
            case "monthly": return startDate.plusMonths(1);
            case "quarterly": return startDate.plusMonths(3);
            case "halfyearly":
            case "half_yearly": return startDate.plusMonths(6);
            case "yearly": return startDate.plusYears(1);
            default: return startDate.plusMonths(1);
        }
    }

    private String buildEmailHtml(
            String ownerName, String shopName, String email,
            String planName, String billingCycle,
            String invoiceNumber, String transactionUuid,
            String gateway, String paidDate,
            String subStart, String subEnd,
            String amount, String tax, String total
    ) {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin:0; padding:0; background:#eef2f7; font-family:'Segoe UI',Arial,sans-serif;">
    <div style="max-width:600px; margin:0 auto; background:#fff; border-radius:12px; overflow:hidden; box-shadow:0 4px 20px rgba(0,0,0,0.08);">

        <!-- Header -->
        <div style="background:linear-gradient(135deg,#1e293b,#0f172a); padding:28px 32px; color:#fff;">
            <div style="display:flex; align-items:center; gap:12px;">
                <div style="width:44px; height:44px; border-radius:10px; background:linear-gradient(135deg,#f97316,#ea580c); display:flex; align-items:center; justify-content:center; font-size:20px; font-weight:800; color:#fff;">M</div>
                <div>
                    <div style="font-size:18px; font-weight:800;">MYCON DIGITAL</div>
                    <div style="font-size:11px; color:#94a3b8;">Software &amp; Technology Solutions</div>
                </div>
            </div>
        </div>

        <!-- Success Banner -->
        <div style="background:#f0fdf4; padding:24px 32px; text-align:center; border-bottom:1px solid #bbf7d0;">
            <div style="width:56px; height:56px; border-radius:50%; background:#dcfce7; display:inline-flex; align-items:center; justify-content:center; margin-bottom:12px;">
                <span style="font-size:28px;">&#10003;</span>
            </div>
            <h2 style="margin:0; font-size:20px; color:#059669;">Payment Successful!</h2>
            <p style="margin:6px 0 0 0; font-size:14px; color:#64748b;">Your subscription has been activated</p>
        </div>

        <!-- Body -->
        <div style="padding:28px 32px;">

            <p style="font-size:14px; color:#1e293b; margin:0 0 20px 0;">Dear <strong>%s</strong>,</p>
            <p style="font-size:14px; color:#64748b; margin:0 0 24px 0; line-height:1.6;">
                Thank you for your payment. Your <strong>%s Plan</strong> subscription is now active.
                Your invoice is attached to this email as a PDF document.
            </p>

            <!-- Invoice Details Card -->
            <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:10px; padding:20px; margin-bottom:24px;">
                <div style="font-size:11px; font-weight:700; letter-spacing:1.5px; text-transform:uppercase; color:#f97316; margin-bottom:14px;">Invoice Details</div>

                <table style="width:100%%; font-size:13px; border-collapse:collapse;">
                    <tr>
                        <td style="padding:6px 0; color:#94a3b8; width:140px;">Invoice Number</td>
                        <td style="padding:6px 0; font-weight:700; color:#1e293b;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding:6px 0; color:#94a3b8;">Transaction ID</td>
                        <td style="padding:6px 0; font-weight:600; color:#1e293b; font-family:monospace; font-size:12px;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding:6px 0; color:#94a3b8;">Plan</td>
                        <td style="padding:6px 0; font-weight:600; color:#1e293b;">%s Plan</td>
                    </tr>
                    <tr>
                        <td style="padding:6px 0; color:#94a3b8;">Billing Cycle</td>
                        <td style="padding:6px 0; font-weight:600; color:#1e293b; text-transform:capitalize;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding:6px 0; color:#94a3b8;">Subscription Period</td>
                        <td style="padding:6px 0; font-weight:600; color:#1e293b;">%s &ndash; %s</td>
                    </tr>
                    <tr>
                        <td style="padding:6px 0; color:#94a3b8;">Payment Gateway</td>
                        <td style="padding:6px 0; font-weight:600; color:#1e293b;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding:6px 0; color:#94a3b8;">Payment Date</td>
                        <td style="padding:6px 0; font-weight:600; color:#1e293b;">%s</td>
                    </tr>
                </table>

                <div style="border-top:2px solid #e2e8f0; margin-top:14px; padding-top:14px;">
                    <table style="width:100%%; font-size:13px; border-collapse:collapse;">
                        <tr>
                            <td style="padding:5px 0; color:#64748b;">Amount</td>
                            <td style="padding:5px 0; text-align:right; font-weight:600; color:#1e293b;">NPR %s</td>
                        </tr>
                        <tr>
                            <td style="padding:5px 0; color:#64748b;">Tax / VAT</td>
                            <td style="padding:5px 0; text-align:right; font-weight:600; color:#1e293b;">NPR %s</td>
                        </tr>
                        <tr>
                            <td style="padding:8px 0; font-weight:700; color:#1e293b; font-size:15px;">TOTAL PAID</td>
                            <td style="padding:8px 0; text-align:right; font-weight:800; color:#ea580c; font-size:17px;">NPR %s</td>
                        </tr>
                    </table>
                </div>
            </div>

            <!-- Attachment Notice -->
            <div style="background:#fff7ed; border:1px solid #fed7aa; border-radius:10px; padding:16px; margin-bottom:24px; text-align:center;">
                <div style="font-size:24px; margin-bottom:6px;">&#128206;</div>
                <div style="font-size:14px; font-weight:600; color:#1e293b;">Invoice PDF Attached</div>
                <div style="font-size:12px; color:#64748b; margin-top:4px;">Please find your invoice attached to this email.</div>
            </div>

            <p style="font-size:12px; color:#94a3b8; margin:0; line-height:1.6;">
                You can also view your subscription anytime from your dashboard.<br/>
                If you have any questions, contact us at support@mycondigital.com.
            </p>
        </div>

        <!-- Footer -->
        <div style="background:#1e293b; padding:20px 32px; text-align:center; color:#94a3b8; font-size:12px;">
            <div style="font-weight:600; color:#f8fafc; margin-bottom:4px;">Thank you for choosing MYCON Digital.</div>
            <div style="margin-bottom:8px;">Swari Sadhan is a product of MYCON Digital.</div>
            <div style="font-size:10px; color:#64748b;">&copy; 2026 MYCON Digital. All rights reserved.</div>
        </div>
    </div>
</body>
</html>
                """.formatted(
                ownerName,
                planName,
                invoiceNumber,
                transactionUuid,
                planName,
                billingCycle,
                subStart, subEnd,
                gateway,
                paidDate,
                amount,
                tax,
                total
        );
    }

    /**
     * Generate a PDF from HTML using OpenHTML-to-PDF.
     */
    private byte[] generatePdfFromHtml(String html) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF from HTML: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    /**
     * Build the invoice HTML for PDF generation (same design as the download invoice page).
     */
    private String buildInvoicePdfHtml(
            String ownerName, String shopName, String email, String phone, String address,
            String planName, String billingCycle,
            String invoiceNumber, String transactionUuid,
            String gateway, String paidDate,
            String subStart, String subEnd,
            String amount, String discountRow, String taxLabel, String tax, String total
    ) {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Inter', 'Segoe UI', Arial, sans-serif; color: #1e293b; font-size: 13px; }
        .invoice-wrap { width: 100%%; }
        .banner { background: #1e293b; padding: 28px 36px; color: #fff; display: flex; justify-content: space-between; align-items: flex-start; }
        .banner .brand-block { display: flex; align-items: flex-start; gap: 12px; }
        .banner .logo-badge { width: 48px; height: 48px; border-radius: 10px; background: #f97316; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 800; color: #fff; }
        .banner .brand-name { font-size: 18px; font-weight: 800; }
        .banner .brand-tag { font-size: 10px; color: #94a3b8; }
        .banner .brand-addr { font-size: 10px; color: #cbd5e1; margin-top: 6px; }
        .banner .brand-vat { font-size: 10px; color: #cbd5e1; }
        .banner .brand-contact { font-size: 10px; color: #cbd5e1; }
        .banner .brand-web { font-size: 10px; color: #cbd5e1; }
        .banner .invoice-badge { text-align: right; }
        .banner .tag { display: inline-block; padding: 4px 12px; border-radius: 16px; background: #d1fae5; color: #059669; font-size: 10px; font-weight: 700; }
        .banner .inv-no { font-size: 14px; font-weight: 700; margin-top: 6px; color: #f8fafc; }
        .banner .inv-date { font-size: 11px; color: #94a3b8; }
        .body { padding: 24px 36px; }
        .section-label { font-size: 10px; font-weight: 700; letter-spacing: 1.5px; text-transform: uppercase; color: #94a3b8; margin-bottom: 14px; }
        .two-col { display: flex; gap: 16px; margin-bottom: 20px; }
        .col { flex: 1; }
        .card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px 16px; }
        .card .card-title { font-size: 9px; font-weight: 700; letter-spacing: 1.2px; text-transform: uppercase; color: #f97316; margin-bottom: 10px; }
        .card .field { margin: 4px 0; font-size: 12px; }
        .card .lbl { display: inline-block; width: 80px; color: #94a3b8; }
        .card .val { color: #1e293b; font-weight: 600; }
        .card .product-line { font-size: 13px; font-weight: 700; margin-bottom: 8px; padding-bottom: 6px; border-bottom: 1px dashed #cbd5e1; }
        .amt-table { width: 100%%; border-collapse: collapse; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; margin-bottom: 20px; }
        .amt-table thead th { background: #1e293b; color: #f1f5f9; font-size: 10px; font-weight: 600; text-transform: uppercase; padding: 10px 16px; text-align: left; }
        .amt-table thead th:last-child { text-align: right; }
        .amt-table tbody td { padding: 10px 16px; font-size: 12px; border-bottom: 1px solid #f1f5f9; }
        .amt-table tbody td:last-child { text-align: right; font-weight: 600; }
        .amt-table tbody tr:last-child td { border-bottom: none; }
        .amt-table .total-row { background: #fff7ed; }
        .amt-table .total-row td { font-weight: 800; font-size: 14px; color: #1e293b; border-top: 2px solid #fdba74; }
        .amt-table .total-row td:last-child { color: #ea580c; font-size: 16px; }
        .amt-table .desc-cell .sub { font-size: 10px; color: #94a3b8; margin-top: 2px; }
        .pay-section { background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 14px 16px; margin-bottom: 20px; }
        .pay-section .pay-title { font-size: 9px; font-weight: 700; letter-spacing: 1.2px; text-transform: uppercase; color: #059669; margin-bottom: 10px; }
        .pay-grid { display: flex; flex-wrap: wrap; gap: 4px 20px; }
        .pay-grid .field { font-size: 12px; margin: 3px 0; }
        .pay-grid .lbl { display: inline-block; width: 110px; color: #64748b; }
        .pay-grid .val { color: #1e293b; font-weight: 600; }
        .pay-grid .val.paid { color: #059669; }
        .footer { background: #1e293b; padding: 16px 36px; text-align: center; color: #94a3b8; }
        .footer .thanks { font-size: 13px; font-weight: 600; color: #f8fafc; margin-bottom: 3px; }
        .footer .product-line { font-size: 11px; margin-bottom: 6px; }
        .footer .copy { font-size: 9px; color: #64748b; }
    </style>
</head>
<body>
    <div class="invoice-wrap">
        <div class="banner">
            <div class="brand-block">
                <div class="logo-badge">M</div>
                <div>
                    <div class="brand-name">MYCON DIGITAL</div>
                    <div class="brand-tag">Software &amp; Technology Solutions</div>
                    <div class="brand-addr">Janakpur-09, Dhanusha, Madhesh, Nepal</div>
                    <div class="brand-vat">PAN/VAT No: 1023042404</div>
                    <div class="brand-contact">Email: support@mycondigital.com | Phone: 9804896396</div>
                    <div class="brand-web">Website: www.mycondigital.com</div>
                </div>
            </div>
            <div class="invoice-badge">
                <div class="tag">PAID</div>
                <div class="inv-no">%s</div>
                <div class="inv-date">%s</div>
            </div>
        </div>

        <div class="body">
            <div class="section-label">Tax Invoice</div>

            <div class="two-col">
                <div class="col">
                    <div class="card">
                        <div class="card-title">Billed To</div>
                        <div class="field"><span class="lbl">Shop Name</span><span class="val">%s</span></div>
                        <div class="field"><span class="lbl">Owner</span><span class="val">%s</span></div>
                        <div class="field"><span class="lbl">Address</span><span class="val">%s</span></div>
                        <div class="field"><span class="lbl">Email</span><span class="val">%s</span></div>
                        <div class="field"><span class="lbl">Phone</span><span class="val">%s</span></div>
                    </div>
                </div>
                <div class="col">
                    <div class="card">
                        <div class="card-title">Product / Service</div>
                        <div class="product-line">Swari Sadhan &mdash; Business Management Software</div>
                        <div class="field"><span class="lbl">Plan</span><span class="val">%s Plan</span></div>
                        <div class="field"><span class="lbl">Billing Cycle</span><span class="val">%s</span></div>
                        <div class="field"><span class="lbl">Period</span><span class="val">%s &ndash; %s</span></div>
                    </div>
                </div>
            </div>

            <table class="amt-table">
                <thead>
                    <tr><th>Description</th><th>Amount</th></tr>
                </thead>
                <tbody>
                    <tr>
                        <td class="desc-cell"><strong>Swari Sadhan %s Plan</strong><div class="sub">Subscription &middot; %s billing cycle</div></td>
                        <td>NPR %s</td>
                    </tr>
                    %s
                    <tr>
                        <td class="desc-cell"><strong>%s</strong></td>
                        <td>NPR %s</td>
                    </tr>
                    <tr class="total-row">
                        <td>TOTAL</td>
                        <td>NPR %s</td>
                    </tr>
                </tbody>
            </table>

            <div class="pay-section">
                <div class="pay-title">Payment Details</div>
                <div class="pay-grid">
                    <div class="field"><span class="lbl">Gateway</span><span class="val">%s</span></div>
                    <div class="field"><span class="lbl">Transaction ID</span><span class="val">%s</span></div>
                    <div class="field"><span class="lbl">Payment Date</span><span class="val">%s</span></div>
                    <div class="field"><span class="lbl">Status</span><span class="val paid">PAID</span></div>
                </div>
            </div>
        </div>

        <div class="footer">
            <div class="thanks">Thank you for choosing MYCON Digital.</div>
            <div class="product-line">Swari Sadhan is a product of MYCON Digital.</div>
            <div class="copy">This is a computer-generated invoice and does not require a signature. &copy; 2026 MYCON Digital.</div>
        </div>
    </div>
</body>
</html>
                """.formatted(
                invoiceNumber,
                paidDate,
                shopName != null ? shopName : "—",
                ownerName,
                address,
                email,
                phone != null ? phone : "—",
                planName,
                billingCycle,
                subStart, subEnd,
                planName,
                billingCycle,
                amount,
                discountRow,
                taxLabel,
                tax,
                total,
                gateway,
                transactionUuid,
                paidDate
        );
    }
}
