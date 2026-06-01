package com.migfora.sales.runner;

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
            Map<String, Object> result = new LinkedHashMap<>();

            // ── BuiltWith scrape ──────────────────────────────────────────────
            try {
                Document builtWith = Jsoup.connect(
                                "https://builtwith.com/" + domain)
                        .userAgent("Mozilla/5.0")
                        .timeout(15000)
                        .get();

                List<String> technologies = new ArrayList<>();
                for (Element el : builtWith.select(".tech-group .technology")) {
                    String techName = el.select(".tech-name").text();
                    if (!techName.isBlank()) technologies.add(techName);
                }
                result.put("builtWith", technologies);
                log.info("BuiltWith found {} technologies | domain={}",
                        technologies.size(), domain);

            } catch (Exception ex) {
                log.warn("BuiltWith scrape failed | domain={} error={}",
                        domain, ex.getMessage());
                result.put("builtWith", List.of());
                result.put("builtWithError", ex.getMessage());
            }

            // ── Wappalyzer public API ─────────────────────────────────────────
            try {
                Document wappalyzer = Jsoup.connect(
                                "https://www.wappalyzer.com/lookup/" + domain + "/")
                        .userAgent("Mozilla/5.0")
                        .timeout(15000)
                        .get();

                List<String> wapTechs = new ArrayList<>();
                for (Element el : wappalyzer.select("[class*='Technology']")) {
                    String name = el.attr("title");
                    if (!name.isBlank()) wapTechs.add(name);
                }
                result.put("wappalyzer", wapTechs);

            } catch (Exception ex) {
                log.warn("Wappalyzer scrape failed | domain={} error={}",
                        domain, ex.getMessage());
                result.put("wappalyzer", List.of());
            }

            // ── Infer from context data ───────────────────────────────────────
            Map<String, String> inferred = new LinkedHashMap<>();

            if (ctx.getServerHeader() != null) {
                inferred.put("webServer", ctx.getServerHeader());
            }
            if (ctx.getPoweredByHeader() != null) {
                inferred.put("language", ctx.getPoweredByHeader());
            }
            if (ctx.getCdnProvider() != null) {
                inferred.put("cdn", ctx.getCdnProvider());
            }

            result.put("inferredFromHeaders", inferred);

            contextService.writeTechStack(
                    task.getInvestigation().getId(),
                    toJson(result)
            );

            markCompleted(task, toJson(result), toJson(result));

        } catch (Exception ex) {
            markFailed(task, "Unexpected error: " + ex.getMessage());
        }
    }
}
