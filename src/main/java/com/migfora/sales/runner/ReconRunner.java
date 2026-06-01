package com.migfora.sales.runner;

import com.migfora.sales.entity.InvestigationContext;
import com.migfora.sales.entity.ReconTask;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 4:56 PM
 */
public interface ReconRunner {

    ReconTask.ReconTaskType supports();

    void run(ReconTask task, InvestigationContext ctx);
}
