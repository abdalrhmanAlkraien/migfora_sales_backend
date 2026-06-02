package com.migfora.sales.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.migfora.sales.entity.InvestigationContext;
import com.migfora.sales.entity.ReconTask;
import com.migfora.sales.entity.ReconTask.ReconTaskType;
import com.migfora.sales.repository.ReconTaskRepository;
import com.migfora.sales.service.InvestigationContextService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 01/06/2026
 * @Time: 2:39 AM
 */
@Component
@Slf4j
public class TechStackRunner extends BaseRunner {


    private final InvestigationContextService contextService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public TechStackRunner(ReconTaskRepository reconTaskRepository,
                           InvestigationContextService contextService) {
        super(reconTaskRepository);
        this.contextService = contextService;
    }

    @Override
    public ReconTaskType supports() {
        return ReconTaskType.TECH_STACK;
    }

    @Override
    public void run(ReconTask task, InvestigationContext ctx) {
        String domain = task.getInvestigation().getDomain();
        markRunning(task);

        try {
            List<String> detected = new ArrayList<>();
            Map<String, String> inferred = new LinkedHashMap<>();
            Map<String, Object> sources = new LinkedHashMap<>();

            // ── 1. Headers analysis ───────────────────────────────────────
            analyzeHeaders(ctx, detected, inferred);
            sources.put("headers", "analyzed");

            // ── 2. HTML source analysis ───────────────────────────────────
            Document doc = null;
            try {
                doc = Jsoup.connect("https://" + domain)
                        .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1)")
                        .timeout(15000)
                        .get();

                analyzeHtml(doc, detected, inferred);
                sources.put("html", "analyzed");
                log.info("HTML analysis complete | domain={}", domain);
            } catch (Exception ex) {
                log.warn("HTML fetch failed | domain={} error={}", domain, ex.getMessage());
                sources.put("html", "failed: " + ex.getMessage());
            }

            // ── 3. JS bundle analysis ─────────────────────────────────────────
            if (doc != null) {
                try {
                    analyzeJsBundles(doc, domain, detected, inferred);
                    sources.put("js_bundles", "analyzed");
                } catch (Exception ex) {
                    log.warn("JS bundle analysis failed | domain={} error={}", domain, ex.getMessage());
                    sources.put("js_bundles", "failed");
                }
            }

            // ── 4. robots.txt analysis ────────────────────────────────────
            try {
                String robots = fetchText("https://" + domain + "/robots.txt");
                analyzeRobots(robots, detected, inferred);
                sources.put("robots.txt", "analyzed");
            } catch (Exception ex) {
                log.warn("robots.txt fetch failed | domain={}", domain);
                sources.put("robots.txt", "not found");
            }

            // ── 5. DNS records analysis ───────────────────────────────────
            analyzeDns(ctx, detected, inferred);
            sources.put("dns", "analyzed");

            try {
                analyzeJsBundles(doc, domain, detected, inferred);
                sources.put("js_bundles", "analyzed");
            } catch (Exception ex) {
                log.warn("JS bundle analysis failed | domain={} error={}", domain, ex.getMessage());
                sources.put("js_bundles", "failed");
            }

            // ── 6. Infrastructure detection ───────────────────────────────────
            detectInfrastructure(ctx, detected, inferred);
            sources.put("infrastructure", "analyzed");

            // Build final result
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("detected",            detected.stream().distinct().toList());
            result.put("inferredFromHeaders", inferred);
            result.put("sources",             sources);

            contextService.writeTechStack(task.getInvestigation().getId(), toJson(result));
            markCompleted(task, toJson(result), toJson(result));

        } catch (Exception ex) {
            markFailed(task, "Unexpected error: " + ex.getMessage());
        }
    }

    // ── Headers analysis ──────────────────────────────────────────────────────

    private void analyzeHeaders(InvestigationContext ctx,
                                List<String> tech,
                                Map<String, String> inferred) {
        if (ctx.getServerHeader() != null) {
            inferred.put("webServer", ctx.getServerHeader());
            String s = ctx.getServerHeader().toLowerCase();
            if (s.contains("nginx"))     tech.add("Nginx");
            if (s.contains("apache"))    tech.add("Apache");
            if (s.contains("iis"))       tech.add("Microsoft IIS");
            if (s.contains("litespeed")) tech.add("LiteSpeed");
            if (s.contains("caddy"))     tech.add("Caddy");
        }
        if (ctx.getPoweredByHeader() != null) {
            inferred.put("language", ctx.getPoweredByHeader());
            String p = ctx.getPoweredByHeader().toLowerCase();
            if (p.contains("php"))      tech.add("PHP");
            if (p.contains("next"))     tech.add("Next.js");
            if (p.contains("express"))  tech.add("Express.js");
            if (p.contains("asp.net")) tech.add("ASP.NET");
        }
        if (ctx.getCdnProvider() != null) {
            inferred.put("cdn", ctx.getCdnProvider());
            tech.add(ctx.getCdnProvider());
        }

        if (ctx.getAllHeaders() != null) {
            try {
                Map<String, Object> headers = objectMapper.readValue(ctx.getAllHeaders(), Map.class);
                headers.forEach((key, value) -> {
                    String k = key.toLowerCase();
                    String v = value != null ? value.toString().toLowerCase() : "";

                    if (k.equals("x-nextjs-cache"))       { tech.add("Next.js"); inferred.put("framework", "Next.js"); }
                    if (k.equals("x-nextjs-prerender"))   { inferred.put("rendering", "SSG/ISR"); }
                    if (k.equals("x-vercel-id"))          { tech.add("Vercel"); inferred.put("hosting", "Vercel"); }
                    if (k.equals("cf-ray"))               { tech.add("Cloudflare"); inferred.put("cdn", "Cloudflare"); }
                    if (k.equals("x-amz-cf-id"))          { tech.add("AWS CloudFront"); inferred.put("cdn", "CloudFront"); }
                    if (k.equals("x-amz-request-id"))     { tech.add("AWS S3"); inferred.put("storage", "S3"); }
                    if (k.contains("x-shopify"))          { tech.add("Shopify"); inferred.put("ecommerce", "Shopify"); }
                    if (k.contains("x-wix"))              { tech.add("Wix"); inferred.put("cms", "Wix"); }
                    if (k.contains("x-squarespace"))      { tech.add("Squarespace"); inferred.put("cms", "Squarespace"); }
                    if (k.equals("x-powered-by")) {
                        if (v.contains("next"))       { tech.add("Next.js"); inferred.put("framework", "Next.js"); }
                        if (v.contains("nuxt"))       { tech.add("Nuxt.js"); inferred.put("framework", "Nuxt.js"); }
                        if (v.contains("gatsby"))     { tech.add("Gatsby"); inferred.put("framework", "Gatsby"); }
                        if (v.contains("wordpress"))  { tech.add("WordPress"); inferred.put("cms", "WordPress"); }
                        if (v.contains("drupal"))     { tech.add("Drupal"); inferred.put("cms", "Drupal"); }
                        if (v.contains("joomla"))     { tech.add("Joomla"); inferred.put("cms", "Joomla"); }
                        if (v.contains("laravel"))    { tech.add("Laravel"); inferred.put("framework", "Laravel"); }
                        if (v.contains("django"))     { tech.add("Django"); inferred.put("framework", "Django"); }
                        if (v.contains("rails"))      { tech.add("Ruby on Rails"); inferred.put("framework", "Rails"); }
                        if (v.contains("express"))    { tech.add("Express.js"); inferred.put("framework", "Express.js"); }
                    }
                    if (k.equals("set-cookie")) {
                        if (v.contains("wordpress") || v.contains("wp-"))  { tech.add("WordPress"); }
                        if (v.contains("shopify"))                           { tech.add("Shopify"); }
                        if (v.contains("laravel_session"))                   { tech.add("Laravel"); }
                        if (v.contains("phpsessid"))                         { tech.add("PHP"); }
                        if (v.contains("asp.net_sessionid"))                 { tech.add("ASP.NET"); }
                        if (v.contains("jsessionid"))                        { tech.add("Java"); }
                    }
                });
            } catch (Exception ex) {
                log.warn("Could not parse allHeaders | error={}", ex.getMessage());
            }
        }
    }

    // ── HTML analysis ─────────────────────────────────────────────────────────

    private void analyzeHtml(Document doc,
                             List<String> tech,
                             Map<String, String> inferred) {
        String html = doc.html().toLowerCase();

        // Meta generator tag
        Element generator = doc.selectFirst("meta[name=generator]");
        if (generator != null) {
            String content = generator.attr("content").toLowerCase();
            inferred.put("generator", generator.attr("content"));
            if (content.contains("wordpress")) { tech.add("WordPress"); inferred.put("cms", "WordPress"); }
            if (content.contains("drupal"))     { tech.add("Drupal"); inferred.put("cms", "Drupal"); }
            if (content.contains("joomla"))     { tech.add("Joomla"); inferred.put("cms", "Joomla"); }
            if (content.contains("ghost"))      { tech.add("Ghost"); inferred.put("cms", "Ghost"); }
            if (content.contains("wix"))        { tech.add("Wix"); inferred.put("cms", "Wix"); }
            if (content.contains("webflow"))    { tech.add("Webflow"); inferred.put("cms", "Webflow"); }
        }

        // Script src patterns
        for (Element script : doc.select("script[src]")) {
            String src = script.attr("src").toLowerCase();
            if (src.contains("react"))           tech.add("React");
            if (src.contains("vue"))             tech.add("Vue.js");
            if (src.contains("angular"))         tech.add("Angular");
            if (src.contains("jquery"))          tech.add("jQuery");
            if (src.contains("_next/static"))  { tech.add("Next.js"); inferred.put("framework", "Next.js"); }
            if (src.contains("nuxt"))            tech.add("Nuxt.js");
            if (src.contains("gatsby"))          tech.add("Gatsby");
            if (src.contains("shopify"))         tech.add("Shopify");
            if (src.contains("gtag") || src.contains("gtm")) tech.add("Google Analytics");
            if (src.contains("cdn.segment"))     tech.add("Segment");
            if (src.contains("intercom"))        tech.add("Intercom");
            if (src.contains("hotjar"))          tech.add("Hotjar");
            if (src.contains("mixpanel"))        tech.add("Mixpanel");
            if (src.contains("hubspot"))       { tech.add("HubSpot"); inferred.put("crm", "HubSpot"); }
            if (src.contains("salesforce"))    { tech.add("Salesforce"); inferred.put("crm", "Salesforce"); }
            if (src.contains("stripe"))        { tech.add("Stripe"); inferred.put("payment", "Stripe"); }
            if (src.contains("paypal"))        { tech.add("PayPal"); inferred.put("payment", "PayPal"); }
            if (src.contains("crisp.chat"))      tech.add("Crisp");
            if (src.contains("tawk.to"))         tech.add("Tawk.to");
            if (src.contains("zendesk"))       { tech.add("Zendesk"); inferred.put("support", "Zendesk"); }
        }

        // Link tags — CSS frameworks and fonts
        for (Element link : doc.select("link[rel=stylesheet]")) {
            String href = link.attr("href").toLowerCase();
            if (href.contains("bootstrap"))     tech.add("Bootstrap");
            if (href.contains("tailwind"))      tech.add("Tailwind CSS");
            if (href.contains("bulma"))         tech.add("Bulma");
            if (href.contains("font-awesome"))  tech.add("Font Awesome");
            if (href.contains("googleapis"))    tech.add("Google Fonts");
        }

        // Inline HTML patterns
        if (html.contains("wp-content") || html.contains("wp-includes")) {
            tech.add("WordPress");
            inferred.put("cms", "WordPress");
        }
        if (html.contains("shopify")) {
            tech.add("Shopify");
            inferred.put("ecommerce", "Shopify");
        }
        if (html.contains("__next")) {
            tech.add("Next.js");
            inferred.put("framework", "Next.js");
        }
        if (html.contains("data-reactroot") || html.contains("__react")) {
            tech.add("React");
        }
        if (html.contains("ng-version") || html.contains("ng-app")) {
            tech.add("Angular");
        }
        if (html.contains("__nuxt")) {
            tech.add("Nuxt.js");
        }
        if (html.contains("data-v-")) {
            tech.add("Vue.js");
        }

        // Open Graph tags — reveals platform
        Element ogSite = doc.selectFirst("meta[property=og:site_name]");
        if (ogSite != null) {
            inferred.put("siteName", ogSite.attr("content"));
        }
    }

    // ── robots.txt analysis ───────────────────────────────────────────────────

    private void analyzeRobots(String robots,
                               List<String> tech,
                               Map<String, String> inferred) {
        if (robots == null) return;
        String r = robots.toLowerCase();
        if (r.contains("wp-admin") || r.contains("wp-content")) {
            tech.add("WordPress");
            inferred.put("cms", "WordPress");
        }
        if (r.contains("/administrator/")) {
            tech.add("Joomla");
            inferred.put("cms", "Joomla");
        }
        if (r.contains("drupal")) {
            tech.add("Drupal");
            inferred.put("cms", "Drupal");
        }
        if (r.contains("shopify")) {
            tech.add("Shopify");
            inferred.put("ecommerce", "Shopify");
        }
        if (r.contains("laravel")) {
            tech.add("Laravel");
        }
    }

    // ── DNS analysis ──────────────────────────────────────────────────────────

    private void analyzeDns(InvestigationContext ctx,
                            List<String> tech,
                            Map<String, String> inferred) {
        // MX records reveal email provider
        if (ctx.getDnsRecords() != null) {
            String dns = ctx.getDnsRecords().toLowerCase();
            if (dns.contains("google") || dns.contains("googlemail")) {
                tech.add("Google Workspace");
                inferred.put("email", "Google Workspace");
            }
            if (dns.contains("outlook") || dns.contains("microsoft")) {
                tech.add("Microsoft 365");
                inferred.put("email", "Microsoft 365");
            }
            if (dns.contains("mailchimp")) {
                tech.add("Mailchimp");
                inferred.put("email", "Mailchimp");
            }
            if (dns.contains("sendgrid")) {
                tech.add("SendGrid");
                inferred.put("email", "SendGrid");
            }
            if (dns.contains("zendesk")) {
                tech.add("Zendesk");
                inferred.put("support", "Zendesk");
            }
        }

        // NS records reveal DNS provider
        if (ctx.getNameservers() != null) {
            String ns = ctx.getNameservers().toLowerCase();
            if (ns.contains("cloudflare"))  inferred.put("dns", "Cloudflare");
            if (ns.contains("awsdns"))      inferred.put("dns", "AWS Route53");
            if (ns.contains("google"))      inferred.put("dns", "Google Cloud DNS");
            if (ns.contains("azure"))       inferred.put("dns", "Azure DNS");
        }
    }

    // ── HTTP fetch helper ─────────────────────────────────────────────────────

    private String fetchText(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (compatible; Googlebot/2.1)")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200)
            throw new RuntimeException("HTTP " + response.statusCode());

        return response.body();
    }

    private void analyzeJsBundles(Document doc,
                                  String domain,
                                  List<String> tech,
                                  Map<String, String> inferred) {
        // Find all script src URLs
        List<String> scriptUrls = new ArrayList<>();
        for (Element script : doc.select("script[src]")) {
            String src = script.attr("abs:src");
            if (!src.isBlank()) scriptUrls.add(src);
        }

        // For Next.js — also check _next/static/chunks/
        boolean isNextJs = tech.contains("Next.js") ||
                inferred.containsValue("Next.js");

        if (isNextJs) {
            // Fetch the main chunk and scan for known library signatures
            String mainChunkUrl = "https://" + domain + "/_next/static/chunks/main.js";
            scriptUrls.add(0, mainChunkUrl);
        }

        // Only analyze first 3 script files to avoid timeout
        int analyzed = 0;
        for (String url : scriptUrls) {
            if (analyzed >= 3) break;
            try {
                String js = fetchText(url);
                detectFromJsContent(js, tech, inferred);
                analyzed++;
            } catch (Exception ex) {
                // skip failed scripts
            }
        }
    }

    private void detectFromJsContent(String js,
                                     List<String> tech,
                                     Map<String, String> inferred) {
        // Analytics & tracking
        if (js.contains("gtag(") || js.contains("GoogleAnalyticsObject")) {
            tech.add("Google Analytics");
            inferred.put("analytics", "Google Analytics");
        }
        if (js.contains("fbq(") || js.contains("facebook.net/en_US/fbevents")) {
            tech.add("Facebook Pixel");
            inferred.put("analytics", "Facebook Pixel");
        }
        if (js.contains("hotjar") || js.contains("hj(")) {
            tech.add("Hotjar");
            inferred.put("heatmap", "Hotjar");
        }
        if (js.contains("mixpanel")) {
            tech.add("Mixpanel");
            inferred.put("analytics", "Mixpanel");
        }
        if (js.contains("segment.com") || js.contains("analytics.js")) {
            tech.add("Segment");
            inferred.put("analytics", "Segment");
        }

        // Payments
        if (js.contains("stripe.com") || js.contains("Stripe(")) {
            tech.add("Stripe");
            inferred.put("payment", "Stripe");
        }
        if (js.contains("paypal.com") || js.contains("paypal.Buttons")) {
            tech.add("PayPal");
            inferred.put("payment", "PayPal");
        }
        if (js.contains("tap.company") || js.contains("tapPayments")) {
            tech.add("Tap Payments");
            inferred.put("payment", "Tap Payments");
        }
        if (js.contains("moyasar") || js.contains("moyasar.com")) {
            tech.add("Moyasar");
            inferred.put("payment", "Moyasar");
        }

        // Chat & support
        if (js.contains("intercom")) {
            tech.add("Intercom");
            inferred.put("support", "Intercom");
        }
        if (js.contains("tawk.to")) {
            tech.add("Tawk.to");
            inferred.put("support", "Tawk.to");
        }
        if (js.contains("crisp.chat")) {
            tech.add("Crisp");
            inferred.put("support", "Crisp");
        }
        if (js.contains("zendesk")) {
            tech.add("Zendesk");
            inferred.put("support", "Zendesk");
        }
        if (js.contains("freshdesk") || js.contains("freshchat")) {
            tech.add("Freshdesk");
            inferred.put("support", "Freshdesk");
        }

        // CRM & marketing
        if (js.contains("hubspot") || js.contains("hs-scripts")) {
            tech.add("HubSpot");
            inferred.put("crm", "HubSpot");
        }
        if (js.contains("salesforce") || js.contains("pardot")) {
            tech.add("Salesforce");
            inferred.put("crm", "Salesforce");
        }

        // JS frameworks
        if (js.contains("React.createElement") || js.contains("__webpack_require__")) {
            tech.add("React");
        }
        if (js.contains("Vue.component") || js.contains("new Vue(")) {
            tech.add("Vue.js");
        }
        if (js.contains("angular.module") || js.contains("ng-version")) {
            tech.add("Angular");
        }
        if (js.contains("jQuery") || js.contains("window.$")) {
            tech.add("jQuery");
        }

        // E-commerce (especially GCC/MENA relevant)
        if (js.contains("salla.sa") || js.contains("salla.com")) {
            tech.add("Salla");
            inferred.put("ecommerce", "Salla");
        }
        if (js.contains("zid.sa") || js.contains("zid.store")) {
            tech.add("Zid");
            inferred.put("ecommerce", "Zid");
        }
        if (js.contains("shopify")) {
            tech.add("Shopify");
            inferred.put("ecommerce", "Shopify");
        }
        if (js.contains("woocommerce")) {
            tech.add("WooCommerce");
            inferred.put("ecommerce", "WooCommerce");
        }
    }

    private void detectInfrastructure(InvestigationContext ctx,
                                      List<String> tech,
                                      Map<String, String> inferred) {

        // ── Cloud provider from IP org ────────────────────────────────
        if (ctx.getIpOrg() != null) {
            String org = ctx.getIpOrg().toLowerCase();
            if (org.contains("amazon") || org.contains("aws")) {
                tech.add("AWS"); inferred.put("cloud", "AWS");
            } else if (org.contains("google")) {
                tech.add("Google Cloud"); inferred.put("cloud", "Google Cloud");
            } else if (org.contains("microsoft") || org.contains("azure")) {
                tech.add("Azure"); inferred.put("cloud", "Microsoft Azure");
            } else if (org.contains("digitalocean")) {
                tech.add("DigitalOcean"); inferred.put("cloud", "DigitalOcean");
            } else if (org.contains("hetzner")) {
                tech.add("Hetzner"); inferred.put("cloud", "Hetzner");
            } else if (org.contains("linode") || org.contains("akamai")) {
                tech.add("Linode/Akamai"); inferred.put("cloud", "Linode");
            } else if (org.contains("vultr")) {
                tech.add("Vultr"); inferred.put("cloud", "Vultr");
            } else if (org.contains("ovh")) {
                tech.add("OVH"); inferred.put("cloud", "OVH");
            } else if (org.contains("cloudflare")) {
                inferred.put("cloud", "Cloudflare (CDN/Proxy)");
            }
        }

        if (ctx.getAllHeaders() == null) return;

        try {
            Map<String, Object> headers = objectMapper
                    .readValue(ctx.getAllHeaders(), Map.class);

            headers.forEach((key, value) -> {
                String k = key.toLowerCase();
                String v = value != null ? value.toString().toLowerCase() : "";

                // ── Deployment platform ───────────────────────────────
                if (k.equals("x-vercel-id"))           { tech.add("Vercel"); inferred.put("deployment", "Vercel"); }
                if (k.equals("x-netlify-request-id") || k.equals("x-netlify")) {
                    tech.add("Netlify"); inferred.put("deployment", "Netlify");
                }
                if (k.contains("x-render-origin"))     { tech.add("Render.com"); inferred.put("deployment", "Render.com"); }
                if (k.equals("fly-request-id"))        { tech.add("Fly.io"); inferred.put("deployment", "Fly.io"); }
                if (k.equals("x-railway-request-id")) { tech.add("Railway"); inferred.put("deployment", "Railway"); }
                if (k.equals("x-heroku-queue-wait") || (k.equals("via") && v.contains("vegur"))) {
                    tech.add("Heroku"); inferred.put("deployment", "Heroku");
                }

                // ── Kubernetes / container signals ────────────────────
                if (k.equals("x-served-by") && v.matches(".*[a-f0-9]{8,}.*")) {
                    inferred.put("deployment", "Kubernetes (pod detected)");
                }
                if (k.equals("x-request-id") && v.matches("[a-f0-9-]{36}")) {
                    inferred.put("requestTracking", "enabled");
                }

                // ── CDN / edge ────────────────────────────────────────
                if (k.equals("cf-ray"))               inferred.put("cdn", "Cloudflare");
                if (k.equals("x-amz-cf-id"))          { inferred.put("cdn", "AWS CloudFront"); tech.add("AWS CloudFront"); }
                if (k.equals("x-fastly-request-id"))  { inferred.put("cdn", "Fastly"); tech.add("Fastly"); }
                if (k.equals("x-akamai-request-id"))  { inferred.put("cdn", "Akamai"); tech.add("Akamai"); }
                if (k.equals("x-cache") && v.contains("hit from cloudfront")) {
                    inferred.put("cdn", "AWS CloudFront");
                }

                // ── Web server / runtime ──────────────────────────────
                if (k.equals("server")) {
                    if (v.contains("openresty"))      { tech.add("OpenResty"); inferred.put("webServer", "OpenResty"); }
                    if (v.contains("tomcat"))         { tech.add("Tomcat"); inferred.put("runtime", "Java/Tomcat"); inferred.put("language", "Java"); }
                    if (v.contains("jetty"))          { tech.add("Jetty"); inferred.put("runtime", "Java/Jetty"); inferred.put("language", "Java"); }
                    if (v.contains("gunicorn"))       { tech.add("Gunicorn"); inferred.put("runtime", "Python/Gunicorn"); inferred.put("language", "Python"); }
                    if (v.contains("uvicorn"))        { tech.add("Uvicorn"); inferred.put("runtime", "Python/Uvicorn"); inferred.put("language", "Python"); }
                    if (v.contains("kestrel"))        { tech.add("Kestrel"); inferred.put("runtime", ".NET/Kestrel"); inferred.put("language", "C#"); }
                    if (v.contains("cowboy"))         { tech.add("Cowboy"); inferred.put("runtime", "Elixir/Erlang"); }
                    if (v.contains("puma"))           { tech.add("Puma"); inferred.put("runtime", "Ruby/Puma"); inferred.put("language", "Ruby"); }
                    if (v.contains("passenger"))      { tech.add("Passenger"); inferred.put("runtime", "Ruby/Passenger"); }
                }

                // ── Database hints from cookies ───────────────────────
                if (k.equals("set-cookie")) {
                    if (v.contains("jsessionid"))          inferred.put("backendHint", "Java (likely Spring/Tomcat)");
                    if (v.contains("phpsessid"))           inferred.put("backendHint", "PHP");
                    if (v.contains("laravel_session"))     inferred.put("backendHint", "PHP/Laravel");
                    if (v.contains("asp.net_sessionid"))   inferred.put("backendHint", "C#/.NET");
                    if (v.contains("_rails_session"))      inferred.put("backendHint", "Ruby on Rails");
                    if (v.contains("connect.sid"))         inferred.put("backendHint", "Node.js/Express");
                    if (v.contains("django"))              inferred.put("backendHint", "Python/Django");
                }
            });

        } catch (Exception ex) {
            log.warn("Infrastructure detection failed | error={}", ex.getMessage());
        }
    }
}
